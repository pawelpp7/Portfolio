package com.portfolio.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.dto.AssetDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PortfolioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -----------------------------------------------
    // Helper
    // -----------------------------------------------

    private AssetDto buildAssetDto(String name, String qty, String purchase, String current) {
        return AssetDto.builder()
                .name(name)
                .quantity(new BigDecimal(qty))
                .purchasePrice(new BigDecimal(purchase))
                .currentPrice(new BigDecimal(current))
                .build();
    }

    // -----------------------------------------------
    // Test 1: POST /assets
    // -----------------------------------------------

    @Test
    @DisplayName("POST /assets powinien zapisać asset i zwrócić 201 z obliczonymi polami")
    void postAsset_shouldReturn201WithCalculatedFields() throws Exception {
        AssetDto dto = buildAssetDto("Apple", "10", "100", "150");

        mockMvc.perform(post("/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    // -----------------------------------------------
    // Test 2: GET /portfolio/summary
    // -----------------------------------------------

    @Test
    @DisplayName("GET /portfolio/summary powinien zwrócić poprawne podsumowanie po dodaniu assetów")
    void getSummary_shouldReturnCorrectSummary() throws Exception {
        // Apple: qty=10, buy=100, now=150 → currentValue=1500, invested=1000, profit=+500, ROI=50%
        AssetDto apple = buildAssetDto("Apple", "10", "100", "150");
        // Tesla: qty=5, buy=200, now=100 → currentValue=500, invested=1000, profit=-500, ROI=-50%
        AssetDto tesla = buildAssetDto("Tesla", "5", "200", "100");

        mockMvc.perform(post("/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(apple)));

        mockMvc.perform(post("/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tesla)));

        mockMvc.perform(get("/portfolio/summary"))
                .andExpect(status().isOk())
                // totalCurrentValue = 1500 + 500 = 2000
                .andExpect(jsonPath("$.totalCurrentValue").value(2000.0000))
                // totalInvestedValue = 1000 + 1000 = 2000
                .andExpect(jsonPath("$.totalInvestedValue").value(2000.0000))
                // totalProfit = 0
                .andExpect(jsonPath("$.totalProfit").value(0.0000))
                // averageROI = (50 + (-50)) / 2 = 0
                .andExpect(jsonPath("$.averageROI").value(0.0000))
                // largestAsset: Apple (1500) > Tesla (500)
                .andExpect(jsonPath("$.largestAssetName").value("Apple"));
    }

    // -----------------------------------------------
    // Test 3: DELETE /assets/{id}
    // -----------------------------------------------

    @Test
    @DisplayName("DELETE /assets/{id} powinien usunąć asset i zwrócić 204")
    void deleteAsset_shouldReturn204() throws Exception {
        AssetDto dto = buildAssetDto("Microsoft", "5", "300", "350");

        String response = mockMvc.perform(post("/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/assets/" + id))
                .andExpect(status().isNoContent());
    }

    // -----------------------------------------------
    // Test 4: Walidacja - brak wymaganych pól
    // -----------------------------------------------

    @Test
    @DisplayName("POST /assets z pustą nazwą powinien zwrócić 400 Bad Request")
    void postAsset_withBlankName_shouldReturn400() throws Exception {
        AssetDto invalid = buildAssetDto("", "10", "100", "150");

        mockMvc.perform(post("/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
