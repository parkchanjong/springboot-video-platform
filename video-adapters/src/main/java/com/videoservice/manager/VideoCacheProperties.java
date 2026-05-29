// 비디오 Redis 캐시와 stampede protection 설정을 바인딩합니다.
package com.videoservice.manager;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "video.cache")
public class VideoCacheProperties {
    private Duration detailTtl = Duration.ofHours(1);
    private boolean stampedeProtectionEnabled = true;
    private Duration lockTtl = Duration.ofSeconds(5);
    private Duration lockWaitTimeout = Duration.ofSeconds(1);
    private Duration lockRetryInterval = Duration.ofMillis(20);

    public Duration getDetailTtl() {
        return detailTtl;
    }

    public void setDetailTtl(Duration detailTtl) {
        this.detailTtl = detailTtl;
    }

    public boolean isStampedeProtectionEnabled() {
        return stampedeProtectionEnabled;
    }

    public void setStampedeProtectionEnabled(boolean stampedeProtectionEnabled) {
        this.stampedeProtectionEnabled = stampedeProtectionEnabled;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        this.lockTtl = lockTtl;
    }

    public Duration getLockWaitTimeout() {
        return lockWaitTimeout;
    }

    public void setLockWaitTimeout(Duration lockWaitTimeout) {
        this.lockWaitTimeout = lockWaitTimeout;
    }

    public Duration getLockRetryInterval() {
        return lockRetryInterval;
    }

    public void setLockRetryInterval(Duration lockRetryInterval) {
        this.lockRetryInterval = lockRetryInterval;
    }
}
