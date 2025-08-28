package com.videoservice.manager;

import com.videoservice.manager.jpa.user.UserJpaEntity;
import com.videoservice.manager.jpa.user.UserJpaRepository;
import com.videoservice.manager.user.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class UserPersistenceAdapter implements LoadUserPort {
    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<User> loadUser(String userId) {
        return userJpaRepository.findById(userId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> loadAllUsers(List<String> userIds) {
        var userJpaEntities = userJpaRepository.findAllById(userIds);
        return StreamSupport.stream(userJpaEntities.spliterator(), false)
                .map(UserJpaEntity::toDomain)
                .toList();
    }
}