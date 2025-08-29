package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.videoservice.manager.domain.user.UserFixtures;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserServiceTest {
    private UserService sut;

    private final LoadUserPort loadUserPort = mock(LoadUserPort.class);

    @BeforeEach
    void setUp() {
        sut = new UserService(loadUserPort);
    }

    @Nested
    class GetUser {
        @Test
        @DisplayName("userId 가 null 이 아니면 해당되는 user 반환")
        void whenUserIdIsNotNullThenReturnUser() {
            var user = UserFixtures.stub();
            given(loadUserPort.loadUser(any())).willReturn(Optional.of(user));

            var result = sut.getUser("userId");

            then(result).isEqualTo(user);
        }

        @Test
        @DisplayName("userId 가 null 이면 null")
        void whenUserIdNotNullThenReturnNull() {
            var result = sut.getUser(null);

            then(result).isEqualTo(null);
        }
    }
}
