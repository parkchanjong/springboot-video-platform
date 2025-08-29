package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.videoservice.manager.jpa.user.UserJpaEntity;
import com.videoservice.manager.jpa.user.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserPersistenceAdapterTest {
    private UserPersistenceAdapter sut;

    private final UserJpaRepository userJpaRepository = mock(UserJpaRepository.class);

    @BeforeEach
    void setUp() {
        sut = new UserPersistenceAdapter(userJpaRepository);
    }

    @Test
    void testLoadUser() {
        // Given
        var userJpaEntity = new UserJpaEntity("userId", "name", "https://example.com/profile.jpg");
        given(userJpaRepository.findById(any())).willReturn(Optional.of(userJpaEntity));

        // When
        var result = sut.loadUser("userId");

        // Then
        then(result)
                .isPresent()
                .hasValueSatisfying(user -> {
                    then(user.getId()).isEqualTo("userId");
                    then(user.getName()).isEqualTo("name");
                });
    }

    @Test
    void testLoadAllUsers() {
        // Given
        var list = List.of(
                new UserJpaEntity("userId", "name", "https://example.com/profile.jpg"),
                new UserJpaEntity("userId2", "name2", "https://example.com/profile2.jpg")
        );
        given(userJpaRepository.findAllById(any())).willReturn(list);

        // When
        var result = sut.loadAllUsers(List.of("userId", "userId2"));

        // Then
        then(result)
                .extracting("id")
                .contains("userId", "userId2");
    }
}