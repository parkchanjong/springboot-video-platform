package com.videoservice.manager.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.videoservice.manager.CouponUseCase;
import com.videoservice.manager.LoadUserPort;
import com.videoservice.manager.UserSessionPort;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponApiController.class)
class CouponApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponUseCase couponUseCase;

    @MockBean
    private UserSessionPort userSessionPort;

    @MockBean
    private LoadUserPort loadUserPort;

    private String authKey;
    private User user;

    @BeforeEach
    void setUp() {
        authKey = UUID.randomUUID().toString();
        user = UserFixtures.stub();

        given(userSessionPort.getUserId(anyString())).willReturn(user.getId());
        given(loadUserPort.loadUser(anyString())).willReturn(Optional.of(user));
    }

    @Test
    @DisplayName("POST /api/v1/coupons issues coupon for authenticated user")
    void testIssueCoupon() throws Exception {
        // Given
        var couponPolicyId = "coupon-policy-id";

        given(userSessionPort.getUserId(authKey)).willReturn(user.getId());
        given(loadUserPort.loadUser(user.getId())).willReturn(Optional.of(user));

        // When & Then
        mockMvc
                .perform(
                        post("/api/v1/coupons")
                                .header(HeaderAttribute.X_AUTH_KEY, authKey)
                                .param("couponPolicyId", couponPolicyId)
                )
                .andExpect(status().isOk());

        verify(couponUseCase).issueCoupon(
                argThat(actual -> {
                    then(actual.getId()).isEqualTo(user.getId());
                    then(actual.getName()).isEqualTo(user.getName());
                    then(actual.getProfileImageUrl()).isEqualTo(user.getProfileImageUrl());
                    return true;
                }),
                eq(couponPolicyId)
        );
    }
}
