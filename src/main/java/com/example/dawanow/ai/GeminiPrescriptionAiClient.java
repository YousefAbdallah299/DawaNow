package com.example.dawanow.ai;

import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.ai.ExtractedPrescription;
import com.example.dawanow.exception.PrescriptionAiUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

public class GeminiPrescriptionAiClient implements PrescriptionAiClient {

    static final String GENERATE_CONTENT_PATH_TEMPLATE = "/v1beta/models/%s:generateContent";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final Logger log = LoggerFactory.getLogger(GeminiPrescriptionAiClient.class);
    private static final Set<String> ROOT_FIELDS = Set.of("medicines");
    private static final Set<String> MEDICINE_FIELDS = Set.of(
            "rawText", "name", "strength", "form", "confidence"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String generateContentPath;

    public GeminiPrescriptionAiClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            String model
    ) {
        if (model == null || !model.matches("[A-Za-z0-9._-]{1,100}")) {
            throw new IllegalArgumentException("Gemini model configuration is invalid");
        }
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.generateContentPath = GENERATE_CONTENT_PATH_TEMPLATE.formatted(model);
        log.info("Gemini prescription client configured: endpointPath={} model={}",
                safeEndpointPath(generateContentPath), safeLogValue(model));
    }

    @Override
    public ExtractedPrescription analyze(
            byte[] image,
            String contentType,
            String language,
            String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw failure(HttpStatus.BAD_REQUEST, "X-Gemini-Api-Key header is required");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(buildRequest(image, contentType, language));
            String responseBody = restClient.post()
                    .uri(generateContentPath)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody == null ? null : objectMapper.readTree(responseBody));
        } catch (PrescriptionAiUnavailableException exception) {
            log.warn("Gemini prescription response failed validation: category=invalid-response");
            throw exception;
        } catch (RestClientResponseException exception) {
            throw mapUpstreamHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw mapResourceFailure(exception);
        } catch (RestClientException exception) {
            log.warn("Gemini prescription request failed: category=client-error type={}",
                    exception.getClass().getSimpleName());
            throw failure(HttpStatus.BAD_GATEWAY, "Could not send the prescription to Gemini");
        } catch (JsonProcessingException exception) {
            log.warn("Gemini prescription response failed validation: category=invalid-response");
            throw invalidResponse();
        }
    }

    private ObjectNode buildRequest(byte[] image, String contentType, String language) {
        ObjectNode request = objectMapper.createObjectNode();

        ObjectNode content = request.putArray("contents").addObject();
        content.put("role", "user");
        ArrayNode parts = content.putArray("parts");
        ObjectNode inlineData = parts.addObject().putObject("inline_data");
        inlineData.put("mime_type", contentType);
        inlineData.put("data", Base64.getEncoder().encodeToString(image));
        parts.addObject().put("text", extractionPrompt(language));

        ObjectNode generationConfig = request.putObject("generationConfig");
        generationConfig.put("responseMimeType", MediaType.APPLICATION_JSON_VALUE);
        generationConfig.set("responseSchema", responseSchema());
        return request;
    }

    private ObjectNode responseSchema() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "OBJECT");
        root.putArray("required").add("medicines");

        ObjectNode medicines = root.putObject("properties").putObject("medicines");
        medicines.put("type", "ARRAY");
        ObjectNode medicine = medicines.putObject("items");
        medicine.put("type", "OBJECT");
        medicine.putArray("required")
                .add("rawText")
                .add("name")
                .add("strength")
                .add("form")
                .add("confidence");

        ObjectNode properties = medicine.putObject("properties");
        properties.putObject("rawText")
                .put("type", "STRING")
                .put("description", "Exact medicine line visible in the prescription, without invention");
        nullableString(properties.putObject("name"), "Medicine name only, or null when unreadable");
        nullableString(properties.putObject("strength"), "Strength with value and unit, or null when absent or unreadable");
        nullableString(properties.putObject("form"), "Dosage form only, or null when absent or unreadable");
        properties.putObject("confidence")
                .put("type", "NUMBER")
                .put("minimum", 0.0)
                .put("maximum", 1.0)
                .put("description", "Confidence in the visual reading, from 0 to 1");
        return root;
    }

    private void nullableString(ObjectNode node, String description) {
        node.put("type", "STRING");
        node.put("nullable", true);
        node.put("description", description);
    }

    private String extractionPrompt(String language) {
        String outputLanguage = "ar".equals(language) ? "Arabic" : "English";
        return """
                Extract medicine lines from this prescription image only.
                Return the normalized medicine name, strength, and dosage form in %s.
                Preserve rawText exactly as it appears in the image.
                Do not guess, complete, or infer unreadable text. Use null for an unreadable name or for an absent or unreadable strength or form.
                Confidence must measure visual legibility from 0.0 to 1.0, not confidence that a product exists.
                Do not return product IDs, database identifiers, dosage instructions, frequency, duration, package counts, or cart quantities.
                Return only data allowed by the supplied JSON schema.
                """.formatted(outputLanguage);
    }

    private ExtractedPrescription parseResponse(JsonNode response) throws JsonProcessingException {
        if (response == null || !response.isObject()) {
            throw unavailable();
        }

        String blockReason = response.path("promptFeedback").path("blockReason").asText(null);
        if (blockReason != null && !blockReason.isBlank()) {
            throw unavailable();
        }

        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw unavailable();
        }

        JsonNode candidate = candidates.get(0);
        String finishReason = candidate.path("finishReason").asText(null);
        if (finishReason != null && !finishReason.isBlank() && !"STOP".equals(finishReason)) {
            throw unavailable();
        }

        String outputText = findOutputText(candidate.path("content").path("parts"));
        if (outputText == null) {
            throw unavailable();
        }

        JsonNode output = objectMapper.readTree(outputText);
        validateExactFields(output, ROOT_FIELDS);
        JsonNode medicines = output.get("medicines");
        if (medicines == null || !medicines.isArray()) {
            throw unavailable();
        }

        List<ExtractedMedicine> extractedMedicines = new ArrayList<>();
        for (JsonNode medicine : medicines) {
            validateExactFields(medicine, MEDICINE_FIELDS);
            String rawText = requiredString(medicine, "rawText");
            String name = nullableString(medicine, "name");
            String strength = nullableString(medicine, "strength");
            String form = nullableString(medicine, "form");
            JsonNode confidenceNode = medicine.get("confidence");
            if (confidenceNode == null || !confidenceNode.isNumber()) {
                throw unavailable();
            }
            double confidence = confidenceNode.doubleValue();
            if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
                throw unavailable();
            }
            extractedMedicines.add(new ExtractedMedicine(rawText, name, strength, form, confidence));
        }
        return new ExtractedPrescription(List.copyOf(extractedMedicines));
    }

    private String findOutputText(JsonNode parts) {
        if (!parts.isArray()) {
            return null;
        }
        for (int partIndex = parts.size() - 1; partIndex >= 0; partIndex--) {
            JsonNode part = parts.get(partIndex);
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            String text = part.path("text").asText(null);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private void validateExactFields(JsonNode node, Set<String> expectedFields) {
        if (node == null || !node.isObject()) {
            throw unavailable();
        }
        Set<String> actualFields = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(actualFields::add);
        if (!actualFields.equals(expectedFields)) {
            throw unavailable();
        }
    }

    private String requiredString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw unavailable();
        }
        return value.textValue();
    }

    private String nullableString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || (!value.isNull() && !value.isTextual())) {
            throw unavailable();
        }
        return value.isNull() ? null : value.textValue();
    }

    private PrescriptionAiUnavailableException unavailable() {
        return invalidResponse();
    }

    private PrescriptionAiUnavailableException invalidResponse() {
        return failure(HttpStatus.BAD_GATEWAY, "Gemini returned an invalid prescription analysis response");
    }

    private PrescriptionAiUnavailableException failure(HttpStatusCode status, String message) {
        return new PrescriptionAiUnavailableException(status, message);
    }

    private PrescriptionAiUnavailableException mapUpstreamHttpFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        ProviderFailure providerFailure = providerFailure(exception);
        String category = switch (status) {
            case 401, 403 -> "authentication";
            case 429 -> "rate-limit";
            case 404 -> "model".equals(providerFailure.fieldHint())
                    ? "model-not-found"
                    : "upstream-http";
            default -> status >= 500
                    ? "provider-server"
                    : providerFailure.authenticationFailure() ? "authentication" : "request-schema";
        };
        log.warn("Gemini prescription request failed: category={} httpStatus={} providerCode={} fieldHint={}",
                category, status, providerFailure.code(), providerFailure.fieldHint());

        return switch (status) {
            case 400 -> failure(HttpStatus.BAD_REQUEST,
                    "Gemini rejected the prescription image or analysis request");
            case 401 -> failure(HttpStatus.UNAUTHORIZED,
                    "The Gemini API key is invalid or was not accepted");
            case 403 -> failure(HttpStatus.FORBIDDEN,
                    "The Gemini API key does not have permission to use this model");
            case 404 -> failure(HttpStatus.NOT_FOUND,
                    "model".equals(providerFailure.fieldHint())
                            ? "The configured Gemini model was not found"
                            : "The configured Gemini API endpoint was not found");
            case 429 -> failure(HttpStatus.TOO_MANY_REQUESTS,
                    "Gemini rate limit or quota was exceeded. Please try again later");
            default -> status >= 500
                    ? failure(HttpStatusCode.valueOf(status),
                            "Gemini is temporarily unavailable")
                    : failure(HttpStatusCode.valueOf(status),
                            "Gemini rejected the prescription analysis request");
        };
    }

    private PrescriptionAiUnavailableException mapResourceFailure(ResourceAccessException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String type = cause == null ? "unknown" : cause.getClass().getSimpleName();
        boolean timeout = cause instanceof java.net.SocketTimeoutException
                || cause instanceof java.net.http.HttpTimeoutException;
        boolean connection = cause instanceof java.net.ConnectException;
        String category = timeout
                ? "timeout"
                : connection ? "connection" : "network";
        log.warn("Gemini prescription request failed: category={} causeType={}", category, type);

        if (timeout) {
            return failure(HttpStatus.GATEWAY_TIMEOUT,
                    "Gemini did not respond before the request timed out");
        }
        if (connection) {
            return failure(HttpStatus.BAD_GATEWAY,
                    "Could not connect to Gemini");
        }
        return failure(HttpStatus.BAD_GATEWAY,
                "A network error occurred while contacting Gemini");
    }

    private ProviderFailure providerFailure(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsByteArray()).path("error");
            String errorStatus = safeProviderCode(error.path("status").asText(null));
            String reason = null;
            JsonNode details = error.path("details");
            if (details.isArray()) {
                for (JsonNode detail : details) {
                    String candidate = safeProviderCode(detail.path("reason").asText(null));
                    if (candidate != null) {
                        reason = candidate;
                        break;
                    }
                }
            }

            String code = reason != null ? reason : errorStatus != null ? errorStatus : "unknown";
            String message = error.path("message").asText("").toLowerCase(java.util.Locale.ROOT);
            boolean authenticationFailure = code.contains("API_KEY")
                    || code.contains("CREDENTIAL")
                    || code.contains("TOKEN")
                    || message.contains("api key")
                    || message.contains("credential");
            return new ProviderFailure(code, authenticationFailure, fieldHint(message));
        } catch (IOException exceptionIgnored) {
            return new ProviderFailure("unknown", false, "unknown");
        }
    }

    private String safeProviderCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toUpperCase(java.util.Locale.ROOT);
        return normalized.matches("[A-Z0-9_]{1,64}") ? normalized : null;
    }

    private String fieldHint(String message) {
        for (String field : List.of(
                "model", "image", "inline_data", "mime_type", "data", "contents", "parts",
                "generationconfig", "generation_config", "responsemimetype", "response_mime_type",
                "responseschema", "response_schema", "schema", "required", "confidence"
        )) {
            if (message.contains(field)) {
                return field;
            }
        }
        return "unknown";
    }

    private String safeLogValue(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]{1,100}") ? value : "invalid";
    }

    private String safeEndpointPath(String value) {
        return value != null
                && value.matches("/v1beta/models/[A-Za-z0-9._-]{1,100}:generateContent")
                ? value
                : "invalid";
    }

    private record ProviderFailure(String code, boolean authenticationFailure, String fieldHint) {
    }
}
