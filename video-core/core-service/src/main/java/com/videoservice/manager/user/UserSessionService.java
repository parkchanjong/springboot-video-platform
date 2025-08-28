package com.videoservice.manager.user;

import com.videoservice.manager.UserSessionPort;
import com.videoservice.manager.UserSessionUseCase;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService implements UserSessionUseCase {
    private final UserSessionPort sessionPort;

    public UserSessionService(UserSessionPort sessionPort) {
        this.sessionPort = sessionPort;
    }

    @Override
    public String getUserId(String authKey) {
        return sessionPort.getUserId(authKey);
    }
}
