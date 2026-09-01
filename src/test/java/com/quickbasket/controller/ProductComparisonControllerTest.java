package com.quickbasket.controller;

import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.ProductComparisonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductComparisonController.class)
class ProductComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductComparisonService comparisonService;

    @Test
    @DisplayName("GET /api/v1/products/search should return 200 OK with ProductSearchResponse JSON")
    void searchProducts_ShouldReturnOkWithOffers() throws Exception {
        NormalizedProductOffer offer = new NormalizedProductOffer(
                "BLINKIT",
                "Blinkit",
                PlatformType.QUICK_COMMERCE,
                new BigDecimal("54.00"),
                new BigDecimal("56.00"),
                new BigDecimal("3.57"),
                true,
                DeliveryEstimate.instant(14),
                "Blinkit Hub",
                "https://blinkit.com/item/123",
                "https://cdn.blinkit.com/img.jpg"
        );

        BestOption bestOption = new BestOption("BLINKIT", new BigDecimal("54.00"), "BLINKIT", 14);
        ProductSearchResponse mockResponse = new ProductSearchResponse("milk", 1, bestOption, List.of(offer), List.of());

        when(comparisonService.searchProducts(anyString(), anyString(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "milk")
                        .param("lat", "12.9716")
                        .param("lng", "77.5946"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is("milk")))
                .andExpect(jsonPath("$.totalResults", is(1)))
                .andExpect(jsonPath("$.bestOption.cheapestPlatformCode", is("BLINKIT")))
                .andExpect(jsonPath("$.offers", hasSize(1)))
                .andExpect(jsonPath("$.offers[0].platformCode", is("BLINKIT")))
                .andExpect(jsonPath("$.offers[0].platformType", is("QUICK_COMMERCE")))
                .andExpect(jsonPath("$.failedProviders", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/products/search with blank query should return 400 Bad Request ProblemDetail")
    void searchProducts_WithBlankQuery_ShouldReturn400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request Parameters")))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
