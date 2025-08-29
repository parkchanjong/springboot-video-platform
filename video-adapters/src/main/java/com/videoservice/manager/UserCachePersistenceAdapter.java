package com.videoservice.manager;

import com.videoservice.manager.jpa.user.UserJpaEntity;
import com.videoservice.manager.jpa.user.UserJpaRepository;
import com.videoservice.manager.redis.user.UserRedisHash;
import com.videoservice.manager.redis.user.UserRedisRepository;
import com.videoservice.manager.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("userCachePersistenceAdapter")
public class UserCachePersistenceAdapter implements LoadUserPort {
    private final UserJpaRepository userJpaRepository;
    private final UserRedisRepository userRedisRepository;

    public UserCachePersistenceAdapter(UserJpaRepository userJpaRepository, UserRedisRepository userRedisRepository) {
        this.userJpaRepository = userJpaRepository;
        this.userRedisRepository = userRedisRepository;
    }

    @Override
    public Optional<User> loadUser(String userId) {
        return userRedisRepository.findById(userId)
                .map(UserRedisHash::toDomain)
                .or(() -> {
                    var optionalEntity = userJpaRepository.findById(userId);
                    optionalEntity.ifPresent(userJpaEntity -> userRedisRepository.save(UserRedisHash.from(userJpaEntity.toDomain())));
                    return optionalEntity.map(UserJpaEntity::toDomain);
                });
    }

    @Override
    public List<User> loadAllUsers(List<String> userIds) {
        return userIds.stream()
                .map(id -> loadUser(id).get())
                .toList();
    }
}