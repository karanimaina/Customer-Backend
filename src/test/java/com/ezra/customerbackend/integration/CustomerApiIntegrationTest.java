package com.ezra.customerbackend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP-level integration tests: full Spring context + reactive Netty + controllers (H2 from test config).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerApiIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .responseTimeout(Duration.ofSeconds(60))
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Test
    @DisplayName("POST /register returns 201 and ApiResponse envelope")
    void register_returnsCreatedEnvelope() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String body = registrationJson("Jane", "Api", "NID-REG-" + suffix, "jane." + suffix + "@test.com", "+25471" + suffix.substring(0, 5));

        webTestClient.post()
                .uri("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").exists()
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.creditProfile.creditScore").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /{id} returns customer after register")
    void getCustomer_afterRegister_returnsOk() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String regBody = registrationJson("Get", "User", "NID-GET-" + suffix, "get." + suffix + "@test.com", "+25472" + suffix.substring(0, 5));

        EntityExchangeResult<byte[]> created = webTestClient.post()
                .uri("/api/v1/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(regBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        JsonNode root = objectMapper.readTree(created.getResponseBody());
        long id = root.path("data").path("id").asLong();
        assertThat(id).isPositive();

        webTestClient.get()
                .uri("/api/v1/customers/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.id")
                .value(v -> assertThat(((Number) v).longValue()).isEqualTo(id));
    }

    @Test
    @DisplayName("GET /{id} returns 404 when customer does not exist")
    void getCustomer_unknown_returns404() {
        webTestClient.get()
                .uri("/api/v1/customers/999999999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.data.code").exists();
    }

    private static String registrationJson(String first, String last, String nationalId, String email, String phone) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "nationalId": "%s",
                  "email": "%s",
                  "phoneNumber": "%s",
                  "preferredChannel": "EMAIL"
                }
                """.formatted(first, last, nationalId, email, phone);
    }
}
