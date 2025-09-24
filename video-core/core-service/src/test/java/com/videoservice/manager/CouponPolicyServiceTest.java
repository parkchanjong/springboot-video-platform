package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.command.CouponPolicyCommand;
import com.videoservice.manager.coupon.CouponPolicy;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CouponPolicyServiceTest {

    private CouponPolicyService sut;

    private final LoadCouponPolicyPort loadCouponPolicyPort = mock(LoadCouponPolicyPort.class);
    private final SaveCouponPolicyPort saveCouponPolicyPort = mock(SaveCouponPolicyPort.class);

    @BeforeEach
    void setUp() {
        sut = new CouponPolicyService(loadCouponPolicyPort, saveCouponPolicyPort);
    }


    @Test
    @DisplayName("createCouponPolicy")
    void testCreateCouponPolicy() {
        // Given
        var command = new CouponPolicyCommand(
                100,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 31, 23, 59));

        // When
        sut.createCouponPolicy(command);

        // Then
        var argumentCaptor = ArgumentCaptor.forClass(CouponPolicy.class);
        verify(saveCouponPolicyPort).saveCouponPolicy(argumentCaptor.capture());
        var savedCouponPolicy = argumentCaptor.getValue();

        then(savedCouponPolicy).isNotNull();
        then(savedCouponPolicy.getId()).isNotBlank();
        then(savedCouponPolicy.getTotalQuantity()).isEqualTo(command.totalQuantity());
        then(savedCouponPolicy.getStartTime()).isEqualTo(command.startTime());
        then(savedCouponPolicy.getEndTime()).isEqualTo(command.endTime());
    }

    @Test
    @DisplayName("loadCouponPolicy")
    void testLoadCouponPolicy() {
        // Given
        var couponPolicyId = "couponPolicyId";
        var couponPolicy = CouponPolicy.builder()
                .id(couponPolicyId)
                .totalQuantity(100)
                .startTime(LocalDateTime.of(2024, 1, 1, 0, 0))
                .endTime(LocalDateTime.of(2024, 1, 31, 23, 59))
                .build();
        given(loadCouponPolicyPort.loadCouponPolicy(couponPolicyId)).willReturn(Optional.of(couponPolicy));

        // When
        var result = sut.loadCouponPolicy(couponPolicyId);

        // Then
        then(result).contains(couponPolicy);
        verify(loadCouponPolicyPort).loadCouponPolicy(couponPolicyId);
    }
}
