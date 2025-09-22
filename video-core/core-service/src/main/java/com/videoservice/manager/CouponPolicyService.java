package com.videoservice.manager;

import com.videoservice.manager.command.CouponPolicyCommand;
import com.videoservice.manager.coupon.CouponPolicy;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CouponPolicyService implements CouponPolicyUseCase {

    private final LoadCouponPolicyPort loadCouponPolicyPort;
    private final SaveCouponPolicyPort saveCouponPolicyPort;

    public CouponPolicyService(LoadCouponPolicyPort loadCouponPolicyPort, SaveCouponPolicyPort saveCouponPolicyPort) {
        this.loadCouponPolicyPort = loadCouponPolicyPort;
        this.saveCouponPolicyPort = saveCouponPolicyPort;
    }

    @Override
    public void createCouponPolicy(CouponPolicyCommand command) {
        var couponPolicy = CouponPolicy.builder()
                .id(UUID.randomUUID().toString())
                .totalQuantity(command.totalQuantity())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .build();

        saveCouponPolicyPort.saveCouponPolicy(couponPolicy);
    }

    @Override
    public CouponPolicy loadCouponPolicy(String couponPolicyId) {
        return loadCouponPolicyPort.loadCouponPolicy(couponPolicyId);
    }
}
