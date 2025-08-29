package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.jpa.user.UserJpaEntity;
import com.videoservice.manager.jpa.user.UserJpaRepository;
import com.videoservice.manager.redis.user.UserRedisHash;
import com.videoservice.manager.redis.user.UserRedisRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserCachePersistenceAdapterTest {
    private UserCachePersistenceAdapter sut;

    private final UserJpaRepository userJpaRepository = mock(UserJpaRepository.class);
    private final UserRedisRepository userRedisRepository = mock(UserRedisRepository.class);

    @BeforeEach
    void setUp() {
        sut = new UserCachePersistenceAdapter(userJpaRepository, userRedisRepository);
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUser {
        @Test
        @DisplayName("Redis 에서 User 을 찾을 수 있으면 UserRedisHash 에서 User 반환")
        void existRedis_returnRedisRepository() {
            var id = "userId";
            given(userRedisRepository.findById(any()))
                    .willReturn(Optional.of(UserRedisHash.from(UserFixtures.stub(id))));

            var result = sut.loadUser(id);

            then(result)
                    .isPresent()
                    .hasValueSatisfying(user -> then(user)
                            .hasFieldOrPropertyWithValue("id", id));
        }

        @Test
        @DisplayName("Redis 에서 user 을 찾을 수 없으면 Jpa 에서 user 반환")
        void notExistRedis_returnJpaRepository() {
            var id = "userId";
            given(userRedisRepository.findById(any()))
                    .willReturn(Optional.empty());
            given(userJpaRepository.findById(any()))
                    .willReturn(Optional.of(UserJpaEntity.from(UserFixtures.stub(id))));

            var result = sut.loadUser(id);

            then(result)
                    .isPresent()
                    .hasValueSatisfying(user -> then(user)
                            .hasFieldOrPropertyWithValue("id", id));
        }

        @Test
        @DisplayName("Redis, Jpa 에서 user 을 찾을 수 없으면 empty Optional 반환")
        void notExist_returnEmpty() {
            var id = "userId";
            given(userRedisRepository.findById(any())).willReturn(Optional.empty());
            given(userJpaRepository.findById(any())).willReturn(Optional.empty());

            var result = sut.loadUser(id);

            then(result)
                    .isNotPresent();
        }
    }

    @Nested
    @DisplayName("loadAllUsers")
    class LoadAllUsers {
        @Test
        @DisplayName("loadUser를 순회 조회")
        void givenIdsThenReturnUsers() {
            var id1 = "userId1";
            var id2 = "userId2";
            given(userRedisRepository.findById(any()))
                    .willReturn(Optional.of(UserRedisHash.from(UserFixtures.stub(id1))), Optional.of(UserRedisHash.from(UserFixtures.stub(id2))));

            var result = sut.loadAllUsers(List.of(id1, id2));

            then(result)
                    .extracting("id")
                    .contains("userId1", "userId2");
        }
    }
}