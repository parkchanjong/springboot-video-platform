package com.videoservice.manager.user;

import com.videoservice.manager.LoadUserPort;
import com.videoservice.manager.UserUserCase;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserUserCase {
    private final LoadUserPort loadUserPort;

    public UserService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public User getUser(String userId) {
        if (userId == null) {
            return null;
        }
        return loadUserPort.loadUser(userId)
                .orElse(null);
    }
}