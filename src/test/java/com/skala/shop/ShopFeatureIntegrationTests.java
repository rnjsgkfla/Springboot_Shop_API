package com.skala.shop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShopFeatureIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void jwtReferralStockAndWishUserJourney() throws Exception {
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "referrer01",
                      "customerPassword": "password123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerPoint").value(1_000_000.0));

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "invited01",
                      "customerPassword": "password123",
                      "referrerId": "referrer01"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerPoint").value(1_010_000.0))
            .andExpect(jsonPath("$.referrerId").value("referrer01"));

        String invitedToken = login("invited01", "password123");
        String referrerToken = login("referrer01", "password123");

        mockMvc.perform(get("/api/customers/me")
                .header("Authorization", bearer(referrerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerPoint").value(1_010_000.0));

        mockMvc.perform(post("/api/customers/order")
                .header("Authorization", bearer(invitedToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerPoint").value(980_000.0))
            .andExpect(jsonPath("$.products[0].quantity").value(2));

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockQuantity").value(98));

        mockMvc.perform(post("/api/customers/cancel")
                .header("Authorization", bearer(invitedToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerPoint").value(995_000.0))
            .andExpect(jsonPath("$.products[0].quantity").value(1));

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockQuantity").value(99));

        mockMvc.perform(post("/api/wishes/2")
                .header("Authorization", bearer(invitedToken)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").value(2));

        mockMvc.perform(post("/api/wishes/2")
                .header("Authorization", bearer(invitedToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WISH_DUPLICATED"));

        mockMvc.perform(get("/api/wishes")
                .header("Authorization", bearer(invitedToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productId").value(2));

        mockMvc.perform(delete("/api/wishes/2")
                .header("Authorization", bearer(invitedToken)))
            .andExpect(status().isNoContent());
    }

    @Test
    void protectedApiRejectsRequestWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/customers/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":1}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    void invalidReferralAndInsufficientStockAreRejected() throws Exception {
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "self01",
                      "customerPassword": "password123",
                      "referrerId": "self01"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REFERRER"));

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "stock01",
                      "customerPassword": "password123"
                    }
                    """))
            .andExpect(status().isCreated());

        String token = login("stock01", "password123");
        mockMvc.perform(post("/api/customers/order")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":101}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    private String login(String customerId, String password) throws Exception {
        String response = mockMvc.perform(post("/api/customers/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "%s",
                      "customerPassword": "%s"
                    }
                    """.formatted(customerId, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
