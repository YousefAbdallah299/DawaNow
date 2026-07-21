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
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiPrescriptionAiClientTest {

    private static final String BASE_URL = "https://gemini.test";
    private static final String API_KEY = "test-key";
    private static final String MODEL = "gemini-test-model";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private GeminiPrescriptionAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiPrescriptionAiClient(builder.build(), objectMapper, MODEL);
    }

    @Test
    void sendsSecureStructuredEnglishRequestAndParsesResponse() throws Exception {
        byte[] image = new byte[]{1, 2, 3, 4};
        String output = """
                {"medicines":[{"rawText":"Abilify 15 mg tablets","name":"Abilify","strength":"15 mg","form":"tablets","confidence":0.96}]}
                """;

        server.expect(once(), requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.input[0].type").value("image"))
                .andExpect(jsonPath("$.input[0].mime_type").value("image/jpeg"))
                .andExpect(jsonPath("$.input[0].data").value(Base64.getEncoder().encodeToString(image)))
                .andExpect(jsonPath("$.input[1].text").value(containsString("in English")))
                .andExpect(jsonPath("$.input[1].text").value(containsString("Do not return product IDs")))
                .andExpect(jsonPath("$.response_format.mime_type").value(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.response_format.schema.properties.medicines.items.properties.confidence.minimum").value(0.0))
                .andExpect(jsonPath("$.response_format.schema.properties.medicines.items.properties.confidence.maximum").value(1.0))
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
        server.verify();
    }

    @Test
    void requestsArabicAndParsesUncertainNullableFields() throws Exception {
        String output = """
                {"medicines":[{"rawText":"دواء غير واضح","name":null,"strength":null,"form":null,"confidence":0.2}]}
                """;
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andExpect(jsonPath("$.input[1].text").value(containsString("in Arabic")))
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
    void rejectsMalformedStructuredOutput() throws Exception {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withSuccess(providerResponse("not-json"), MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsBlockedProviderOutput() {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withSuccess("{\"status\":\"failed\",\"steps\":[]}", MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsCompletedResponseWithoutModelOutput() {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withSuccess("{\"status\":\"completed\",\"steps\":[]}", MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void rejectsOutOfRangeConfidenceAndUnexpectedFields() throws Exception {
        String output = """
                {"medicines":[{"rawText":"Panadol","name":"Panadol","strength":null,"form":null,"confidence":1.2,"productId":438}]}
                """;
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withSuccess(providerResponse(output), MediaType.APPLICATION_JSON));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 429, 500, 503})
    void mapsProviderHttpErrorsToUnavailable(int status) {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void mapsInvalidPerRequestKeyAuthenticationFailureToUnavailable() {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andExpect(header("x-goog-api-key", "invalid-key"))
                .andRespond(withStatus(HttpStatusCode.valueOf(401)));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", "invalid-key"));
    }

    @Test
    void rejectsMissingOrEmptyPerRequestKeyWithoutCallingGemini() {
        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", " "))
                .isInstanceOf(PrescriptionAiUnavailableException.class)
                .hasMessage("Prescription AI is not configured");
        server.verify();
    }

    @Test
    void mapsTimeoutToUnavailable() {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withException(new SocketTimeoutException("timed out")));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    @Test
    void mapsConnectionFailureToUnavailable() {
        server.expect(requestTo(BASE_URL + GeminiPrescriptionAiClient.INTERACTIONS_PATH))
                .andRespond(withException(new ConnectException("connection refused")));

        assertUnavailable(() -> client.analyze(new byte[]{1}, "image/jpeg", "en", API_KEY));
    }

    private String providerResponse(String output) throws JsonProcessingException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "completed");
        ObjectNode step = response.putArray("steps").addObject();
        step.put("type", "model_output");
        ObjectNode content = step.putArray("content").addObject();
        content.put("type", "text");
        content.put("text", output);
        return objectMapper.writeValueAsString(response);
    }

    private void assertUnavailable(ThrowingCall call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(PrescriptionAiUnavailableException.class)
                .hasMessage("Prescription AI provider is unavailable");
        server.verify();
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}
