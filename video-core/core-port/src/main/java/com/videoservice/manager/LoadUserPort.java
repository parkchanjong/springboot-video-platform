package com.videoservice.manager;

import com.videoservice.manager.user.User;
import java.util.List;
import java.util.Optional;

public interface LoadUserPort {
    Optional<User> loadUser(String userId);

    List<User> loadAllUsers(List<String> userIds);
}