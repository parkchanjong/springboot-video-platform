package com.videoservice.manager.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoservice.manager.CouponPolicyUseCase;
import com.videoservice.manager.coupon.CouponPolicy;
import com.videoservice.manager.dto.CouponPolicyRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponPolicyApiController.class)
class CouponPolicyApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponPolicyUseCase couponPolicyUseCase;

    @Test
    @DisplayName("POST /api/v1/coupon-policies delegates to create coupon policy use case")
    void testCreateCouponPolicy() throws Exception {
        // Given
        var startTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        var endTime = LocalDateTime.of(2024, 12, 31, 23, 59);
        var request = new CouponPolicyRequest(100, startTime, endTime);

        // When & Then
        mockMvc
                .perform(
                        post("/api/v1/coupon-policies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(couponPolicyUseCase).createCouponPolicy(argThat(command -> {
            then(command.totalQuantity()).isEqualTo(100);
            then(command.startTime()).isEqualTo(startTime);
            then(command.endTime()).isEqualTo(endTime);
            return true;
        }));
    }

    @Test
    @DisplayName("GET /api/v1/coupon-policies/{couponPolicyId} returns coupon policy")
    void testGetCouponPolicy() throws Exception {
        // Given
        var couponPolicyId = "policy-123";
        var startTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        var endTime = LocalDateTime.of(2024, 12, 31, 23, 59);
        var couponPolicy = CouponPolicy.builder()
                .id(couponPolicyId)
                .totalQuantity(50)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        given(couponPolicyUseCase.loadCouponPolicy(couponPolicyId)).willReturn(couponPolicy);

        // When & Then
        mockMvc
                .perform(get("/api/v1/coupon-policies/{couponPolicyId}", couponPolicyId))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(couponPolicyId),
                        jsonPath("$.totalQuantity").value(50),
                        jsonPath("$.startTime").value(startsWith("2024-01-01T00:00")),
                        jsonPath("$.endTime").value(startsWith("2024-12-31T23:59"))
                );

        verify(couponPolicyUseCase).loadCouponPolicy(couponPolicyId);
    }
}
