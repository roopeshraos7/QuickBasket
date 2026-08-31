package com.quickbasket.controller;

import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.ProductComparisonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductComparisonController.class)
class ProductComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductComparisonService searchService;

    @Test
    @DisplayName("GET /api/v1/products/search should return 200 OK with product offers JSON")
    void searchProducts_ShouldReturn200OK() throws Exception {
        ProductSearchResponse mockResponse = new ProductSearchResponse(
                "Milk",
                1,
                new BestOption("BLINKIT", new BigDecimal("54.00"), "BLINKIT", 14),
                List.of(new NormalizedProductOffer(
                        "BLINKIT", "Blinkit", new BigDecimal("54.00"), new BigDecimal("56.00"),
                        new BigDecimal("3.57"), true, 14, "http://link", "http://img"
                ))
        );

        when(searchService.searchProducts(anyString(), anyString(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "Milk")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("Milk"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.bestOption.cheapestPlatformCode").value("BLINKIT"))
                .andExpect(jsonPath("$.offers[0].platformCode").value("BLINKIT"));
    }

    @Test
    @DisplayName("GET /api/v1/products/search with blank query should return 400 Bad Request ProblemDetail")
    void searchProducts_WithBlankQuery_ShouldReturn400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "  ")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Parameters"))
                .andExpect(jsonPath("$.detail").value("Search query parameter 'q' cannot be blank."));
    }
}
