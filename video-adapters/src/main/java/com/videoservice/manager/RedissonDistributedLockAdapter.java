package com.videoservice.manager;

import com.videoservice.manager.redis.common.RedisKeyGenerator;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class RedissonDistributedLockAdapter implements DistributedLockPort {

    private final RedissonClient redissonClient;

    public RedissonDistributedLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public DistributedLock getLock(String couponPolicyId) {
        RLock lock = redissonClient.getLock(RedisKeyGenerator.buildLockKey(couponPolicyId));
        return new RedissonLock(lock);
    }

    private static final class RedissonLock implements DistributedLock {

        private final RLock delegate;

        private RedissonLock(RLock delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean tryLock(long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
            return delegate.tryLock(waitTime, leaseTime, timeUnit);
        }

        @Override
        public boolean isHeldByCurrentThread() {
            return delegate.isHeldByCurrentThread();
        }

        @Override
        public void unlock() {
            delegate.unlock();
        }
    }
}
