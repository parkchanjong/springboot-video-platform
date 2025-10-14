package com.videoservice.manager.controller;

import static com.videoservice.manager.RestDocsUtils.requestPreprocessor;
import static com.videoservice.manager.RestDocsUtils.responsePreprocessor;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import com.videoservice.manager.CouponUseCase;
import com.videoservice.manager.RestDocsTest;
import com.videoservice.manager.attribute.HeaderAttribute;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CouponApiControllerTest extends RestDocsTest {

    private CouponUseCase couponUseCase;

    private CouponApiController controller;

    @BeforeEach
    void setUp() {
        couponUseCase = mock(CouponUseCase.class);
        controller = new CouponApiController(couponUseCase);
        mockMvc = mockController(controller);
    }

    @Test
    @DisplayName("POST /api/v1/coupons issues a coupon for the authenticated user")
    void issueCoupon() {
        var couponPolicyId = "coupon-policy-id";

        given().contentType(ContentType.JSON)
                .header(HeaderAttribute.X_AUTH_KEY, "auth-key-001")
                .queryParam("couponPolicyId", couponPolicyId)
                .post("/api/v1/coupons")
                .then()
                .status(HttpStatus.OK)
                .apply(document("coupons-issue", requestPreprocessor(), responsePreprocessor(),
                        requestHeaders(
                                headerWithName(HeaderAttribute.X_AUTH_KEY).description(
                                        "Authentication key for the current user")),
                        queryParameters(
                                parameterWithName("couponPolicyId").description(
                                        "Identifier of the coupon policy to issue"))));
    }
}
