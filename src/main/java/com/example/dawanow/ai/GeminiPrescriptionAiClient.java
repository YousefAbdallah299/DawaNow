package com.example.dawanow.ai;

import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.ai.ExtractedPrescription;
import com.example.dawanow.exception.PrescriptionAiUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class GeminiPrescriptionAiClient implements PrescriptionAiClient {

    static final String INTERACTIONS_PATH = "/v1beta/interactions";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final Set<String> ROOT_FIELDS = Set.of("medicines");
    private static final Set<String> MEDICINE_FIELDS = Set.of(
            "rawText", "name", "strength", "form", "confidence"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public GeminiPrescriptionAiClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            String model
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public ExtractedPrescription analyze(
            byte[] image,
            String contentType,
            String language,
            String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new PrescriptionAiUnavailableException("Prescription AI is not configured");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(buildRequest(image, contentType, language));
            String responseBody = restClient.post()
                    .uri(INTERACTIONS_PATH)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody == null ? null : objectMapper.readTree(responseBody));
        } catch (PrescriptionAiUnavailableException exception) {
            throw exception;
        } catch (RestClientException | JsonProcessingException exception) {
            throw unavailable();
        }
    }

    private ObjectNode buildRequest(byte[] image, String contentType, String language) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("store", false);

        ArrayNode input = request.putArray("input");
        ObjectNode imageInput = input.addObject();
        imageInput.put("type", "image");
        imageInput.put("data", Base64.getEncoder().encodeToString(image));
        imageInput.put("mime_type", contentType);

        ObjectNode textInput = input.addObject();
        textInput.put("type", "text");
        textInput.put("text", extractionPrompt(language));

        ObjectNode responseFormat = request.putObject("response_format");
        responseFormat.put("type", "text");
        responseFormat.put("mime_type", MediaType.APPLICATION_JSON_VALUE);
        responseFormat.set("schema", responseSchema());
        return request;
    }

    private ObjectNode responseSchema() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.putArray("required").add("medicines");

        ObjectNode medicines = root.putObject("properties").putObject("medicines");
        medicines.put("type", "array");
        ObjectNode medicine = medicines.putObject("items");
        medicine.put("type", "object");
        medicine.put("additionalProperties", false);
        medicine.putArray("required")
                .add("rawText")
                .add("name")
                .add("strength")
                .add("form")
                .add("confidence");

        ObjectNode properties = medicine.putObject("properties");
        properties.putObject("rawText")
                .put("type", "string")
                .put("description", "Exact medicine line visible in the prescription, without invention");
        nullableString(properties.putObject("name"), "Medicine name only, or null when unreadable");
        nullableString(properties.putObject("strength"), "Strength with value and unit, or null when absent or unreadable");
        nullableString(properties.putObject("form"), "Dosage form only, or null when absent or unreadable");
        properties.putObject("confidence")
                .put("type", "number")
                .put("minimum", 0.0)
                .put("maximum", 1.0)
                .put("description", "Confidence in the visual reading, from 0 to 1");
        return root;
    }

    private void nullableString(ObjectNode node, String description) {
        node.putArray("type").add("string").add("null");
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
        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw unavailable();
        }

        String outputText = findOutputText(response.path("steps"));
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

    private String findOutputText(JsonNode steps) {
        if (!steps.isArray()) {
            return null;
        }
        for (int stepIndex = steps.size() - 1; stepIndex >= 0; stepIndex--) {
            JsonNode step = steps.get(stepIndex);
            if (!"model_output".equals(step.path("type").asText())) {
                continue;
            }
            JsonNode content = step.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (int contentIndex = content.size() - 1; contentIndex >= 0; contentIndex--) {
                JsonNode part = content.get(contentIndex);
                if ("text".equals(part.path("type").asText())) {
                    String text = part.path("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
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
        return new PrescriptionAiUnavailableException("Prescription AI provider is unavailable");
    }
}
