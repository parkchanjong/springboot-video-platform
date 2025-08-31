package com.videoservice.manager.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.videoservice.manager.LoadUserPort;
import com.videoservice.manager.UserSessionPort;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.user.User;
import java.util.Optional;
import org.springframework.boot.test.mock.mockito.MockBean;

public class AuthBaseControllerTest {
    @MockBean
    private UserSessionPort userSessionPort;
    @MockBean
    private LoadUserPort loadUserPort;

    private User user;

    void prepareUser() {
        user = UserFixtures.stub();

        given(userSessionPort.getUserId(anyString())).willReturn("userId");
        given(loadUserPort.loadUser(anyString())).willReturn(Optional.of(user));
    }
}
