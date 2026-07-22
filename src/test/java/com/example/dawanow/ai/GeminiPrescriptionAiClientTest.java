package com.example.dawanow.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.dawanow.exception.PrescriptionAiUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class GeminiPrescriptionAiClientTest {

    private static final String BASE_URL = "https://gemini.test";
    private static final String API_KEY = "test-key";
    private static final String MODEL = "gemini-test-model";
    private static final String GENERATE_CONTENT_URL = BASE_URL
            + GeminiPrescriptionAiClient.GENERATE_CONTENT_PATH_TEMPLATE.formatted(MODEL);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private GeminiPrescriptionAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiPrescriptionAiClient(builder.build(), objectMapper, MODEL, BASE_URL, false);
    }

    @Test
    void sendsSecureStructuredEnglishRequestAndParsesResponse(CapturedOutput capturedOutput) throws Exception {
        byte[] image = new byte[]{1, 2, 3, 4};
        String output = """
                {"medicines":[{"rawText":"Abilify 15 mg tablets","name":"Abilify","strength":"15 mg","form":"tablets","confidence":0.96}]}
                """;

        server.expect(once(), requestTo(GENERATE_CONTENT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.model").doesNotExist())
                .andExpect(jsonPath("$.store").doesNotExist())
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.mime_type").value("image/jpeg"))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.data")
                        .value(Base64.getEncoder().encodeToString(image)))
                .andExpect(jsonPath("$.contents[0].parts[1].text").value(containsString("in English")))
                .andExpect(jsonPath("$.contents[0].parts[1].text")
                        .value(containsString("Do not return product IDs")))
                .andExpect(jsonPath("$.generationConfig.responseMimeType")
                        .value(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("OBJECT"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.medicines.items.properties.name.nullable")
                        .value(true))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.medicines.items.properties.confidence.minimum")
                        .value(0.0))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.medicines.items.properties.confidence.maximum")
                        .value(1.0))
                .andExpect(content().string(not(containsString("productId"))))
                .andExpect(content().string(not(containsString("\"quantity\""))))
                .andRespond(withSuccess(providerResponse(output), MediaType.APPLICATION_JSON));

        var result = client.analyze(image, "image/jpeg", "en", API_KEY);

        assertThat(result.medicines()).hasSize(1);
        assertThat(result.medicines().getFirst().rawText()).isEqualTo("Abilify 15 mg tablets");
        assertThat(result.medicines().getFirst().name()).isEqualTo("Abilify");
        assertThat(result.medicines().getFirst().strength()).isEqualTo("15 mg");
        assertThat(result.medicines().getFirst().form()).isEqualTo("tablets");
        assertThat(result.medicines().getFirst().confidence()).isEqualTo(0.96);
        assertThat(capturedOutput)
                .contains("Gemini prescription request configuration")
                .contains("endpointUrl=" + GENERATE_CONTENT_URL)
                .contains("model=" + MODEL)
                .contains("apiVersion=v1beta")
                .contains("geminiModelEnvironmentOverride=false")
                .doesNotContain(API_KEY)
                .doesNotContain(Base64.getEncoder().encodeToString(image));
        server.verify();
    }

    @Test
    void requestsArabicAndParsesUncertainNullableFields() throws Exception {
        String output = """
                {"medicines":[{"rawText":"دواء غير واضح","name":null,"strength":null,"form":null,"confidence":0.2}]}
                """;
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.mime_type").value("image/png"))
                .andExpect(jsonPath("$.contents[0].parts[1].text").value(containsString("in Arabic")))
                .andRespond(withSuccess(providerResponse(output), MediaType.APPLICATION_JSON));

        var medicine = client.analyze(new byte[]{1}, "image/png", "ar", API_KEY).medicines().getFirst();

        assertThat(medicine.rawText()).isEqualTo("دواء غير واضح");
        assertThat(medicine.name()).isNull();
        assertThat(medicine.strength()).isNull();
        assertThat(medicine.form()).isNull();
        assertThat(medicine.confidence()).isEqualTo(0.2);
        server.verify();
    }

    @Test
    void parsesGenerateContentResponseWithThoughtPartBeforeStructuredOutput() throws Exception {
        String output = """
                {"medicines":[{"rawText":"Panadol Extra","name":"Panadol Extra","strength":null,"form":null,"confidence":0.91}]}
                """;
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess(providerResponseWithThought(output), MediaType.APPLICATION_JSON));

        var result = client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY);

        assertThat(result.medicines()).singleElement()
                .satisfies(medicine -> assertThat(medicine.name()).isEqualTo("Panadol Extra"));
        server.verify();
    }

    @Test
    void rejectsMalformedStructuredOutput() throws Exception {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess(providerResponse("not-json"), MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsBlockedPromptOutput() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"},\"candidates\":[]}",
                        MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsResponseWithoutCandidates() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsCandidateWithoutTextOutput() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[]}}]}",
                        MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsNonSuccessfulFinishReason() throws Exception {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess(providerResponse("{}", "MAX_TOKENS"), MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsOutOfRangeConfidenceAndUnexpectedFields() throws Exception {
        String output = """
                {"medicines":[{"rawText":"Panadol","name":"Panadol","strength":null,"form":null,"confidence":1.2,"productId":438}]}
                """;
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess(providerResponse(output), MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 429, 500, 503})
    void preservesSafeProviderHttpStatusInsteadOfChangingEveryFailureTo503(int status) {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        assertFailure(
                () -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY),
                HttpStatusCode.valueOf(status)
        );
    }

    @Test
    void mapsInvalidPerRequestKeyAuthenticationFailureToUnavailable() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andExpect(header("x-goog-api-key", "invalid-key"))
                .andRespond(withStatus(HttpStatusCode.valueOf(401)));

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", "invalid-key"))
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("The Gemini API key is invalid or was not accepted");
                });
        server.verify();
    }

    @Test
    void classifiesMissingConfiguredModelWithoutExposingProviderResponse(CapturedOutput output) {
        String providerResponse = """
                {"error":{"code":404,"message":"Configured model was not found","status":"NOT_FOUND"}}
                """;
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(404))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(providerResponse));

        assertFailure(
                () -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY),
                HttpStatus.NOT_FOUND
        );

        assertThat(output).contains("category=model-not-found")
                .doesNotContain(providerResponse);
    }

    @Test
    void classifiesUnsupportedImageWithoutExposingProviderResponse(CapturedOutput output) {
        String providerResponse = """
                {"error":{"code":400,"message":"Image input is not supported", "status":"INVALID_ARGUMENT", "details":[{"reason":"IMAGE_NOT_SUPPORTED"}]}}
                """;
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(400))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(providerResponse));

        assertFailure(
                () -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY),
                HttpStatus.BAD_REQUEST
        );

        assertThat(output).contains("category=request-schema")
                .contains("httpStatus=400")
                .contains("providerCode=IMAGE_NOT_SUPPORTED")
                .contains("fieldHint=image")
                .doesNotContain(providerResponse)
                .doesNotContain(API_KEY)
                .doesNotContain(Base64.getEncoder().encodeToString(new byte[]{1}));
    }

    @Test
    void rejectsMissingOrEmptyPerRequestKeyWithoutCallingGemini() {
        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", " "))
                .isInstanceOf(PrescriptionAiUnavailableException.class)
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("X-Gemini-Api-Key header is required");
                });
        server.verify();
    }

    @Test
    void mapsTimeoutToUnavailable() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withException(new SocketTimeoutException("timed out")));

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY))
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                    assertThat(exception.getMessage()).contains("timed out");
                });
        server.verify();
    }

    @Test
    void mapsJavaHttpClientTimeoutToGatewayTimeout() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withException(new HttpTimeoutException("timed out")));

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY))
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                    assertThat(exception.getMessage()).contains("timed out");
                });
        server.verify();
    }

    @Test
    void mapsConnectionFailureToUnavailable() {
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withException(new ConnectException("connection refused")));

        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY))
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getMessage()).isEqualTo("Could not connect to Gemini");
                });
        server.verify();
    }

    private String providerResponse(String output) throws JsonProcessingException {
        return providerResponse(output, "STOP");
    }

    private String providerResponse(String output, String finishReason) throws JsonProcessingException {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode candidate = response.putArray("candidates").addObject();
        candidate.put("finishReason", finishReason);
        candidate.putObject("content").putArray("parts").addObject().put("text", output);
        return objectMapper.writeValueAsString(response);
    }

    private String providerResponseWithThought(String output) throws JsonProcessingException {
        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode candidate = response.putArray("candidates").addObject();
        candidate.put("finishReason", "STOP");
        var parts = candidate.putObject("content").putArray("parts");
        parts.addObject().put("thought", true).put("text", "private model thought");
        parts.addObject().put("text", output);
        return objectMapper.writeValueAsString(response);
    }

    private void assertUnavailable(ThrowingCall call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getMessage()).isEqualTo(
                            "Gemini returned an invalid prescription analysis response");
                });
        server.verify();
    }

    private void assertFailure(ThrowingCall call, HttpStatusCode expectedStatus) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(PrescriptionAiUnavailableException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(expectedStatus));
        server.verify();
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}
