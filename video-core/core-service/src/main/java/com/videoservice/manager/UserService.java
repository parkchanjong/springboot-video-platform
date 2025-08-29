package com.videoservice.manager;

import com.videoservice.manager.user.User;
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