package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

import com.videoservice.manager.CouponPolicyUseCase;
import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.domain.coupon.CouponPolicyFixtures;
import com.videoservice.manager.dto.CouponPolicyRequest;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;

class CouponPolicyApiControllerTest extends RestDocsTest {

    private CouponPolicyUseCase couponPolicyUseCase;

    private CouponPolicyApiController controller;

    @BeforeEach
    void setUp() {
        couponPolicyUseCase = mock(CouponPolicyUseCase.class);
        controller = new CouponPolicyApiController(couponPolicyUseCase);

        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("POST /api/v1/coupon-policies creates a coupon policy")
    void createCouponPolicy() {
        var request = new CouponPolicyRequest(
                100,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59)
        );

        given().contentType(ContentType.JSON)
                .body(request)
                .post("/api/v1/coupon-policies")
                .then()
                .status(HttpStatus.OK)
                .apply(document("coupon-policies-create", requestPreprocessor(),
                        responsePreprocessor(),
                        requestFields(
                                fieldWithPath("totalQuantity").type(JsonFieldType.NUMBER)
                                        .description("Maximum number of coupons"),
                                fieldWithPath("startTime").type(JsonFieldType.STRING)
                                        .description("Coupon issuance start time (ISO-8601)"),
                                fieldWithPath("endTime").type(JsonFieldType.STRING)
                                        .description("Coupon issuance end time (ISO-8601)"))));

        verify(couponPolicyUseCase).createCouponPolicy(request.toCommand());
    }

    @Test
    @DisplayName("GET /api/v1/coupon-policies/{couponPolicyId} returns a coupon policy")
    void getCouponPolicy() {
        var couponPolicyId = "policy-123";
        when(couponPolicyUseCase.loadCouponPolicy(couponPolicyId)).thenReturn(
                CouponPolicyFixtures.stub(couponPolicyId));

        given().contentType(ContentType.JSON)
                .get("/api/v1/coupon-policies/{couponPolicyId}", couponPolicyId)
                .then()
                .status(HttpStatus.OK)
                .apply(document("coupon-policies-get", requestPreprocessor(),
                        responsePreprocessor(),
                        pathParameters(
                                parameterWithName("couponPolicyId").description(
                                        "Coupon policy identifier")),
                        responseFields(
                                fieldWithPath("id").type(JsonFieldType.STRING)
                                        .description("Coupon policy identifier"),
                                fieldWithPath("totalQuantity").type(JsonFieldType.NUMBER)
                                        .description("Maximum number of coupons"),
                                fieldWithPath("startTime").type(JsonFieldType.STRING)
                                        .description("Coupon issuance start time (ISO-8601)"),
                                fieldWithPath("endTime").type(JsonFieldType.STRING)
                                        .description("Coupon issuance end time (ISO-8601)"))));
    }
}
