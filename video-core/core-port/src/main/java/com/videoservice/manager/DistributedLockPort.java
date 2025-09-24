package com.videoservice.manager;

import java.util.concurrent.TimeUnit;

public interface DistributedLockPort {

    DistributedLock getLock(String couponPolicyId);

    interface DistributedLock {
        boolean tryLock(long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException;
        boolean isHeldByCurrentThread();
        void unlock();
    }
}
