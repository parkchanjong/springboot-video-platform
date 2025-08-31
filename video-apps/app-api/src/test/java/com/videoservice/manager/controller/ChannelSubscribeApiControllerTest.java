package com.videoservice.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.videoservice.manager.SubscribeUseCase;
import com.videoservice.manager.UserUserCase;
import com.videoservice.manager.attribute.HeaderAttribute;
import com.videoservice.manager.domain.channel.ChannelFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChannelSubscribeApiController.class)
class ChannelSubscribeApiControllerTest extends AuthBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscribeUseCase subscribeUseCase;
    @MockBean
    private UserUserCase userUserCase;

    @BeforeEach
    void setUp() {
        prepareUser();
    }

    @Test
    void subscribeChannel() throws Exception {
        given(subscribeUseCase.subscribeChannel(anyString(), anyString())).willReturn(
                "subscribeId");

        mockMvc
                .perform(
                        post("/api/v1/subscribe?channelId={channelId}", "channelId")
                                .header(HeaderAttribute.X_AUTH_KEY, UUID.randomUUID().toString())
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value("subscribeId")
                );
    }

    @Test
    void listSubscribeChannelByUser() throws Exception {
        var list = IntStream.range(1, 4)
                .mapToObj(i -> ChannelFixtures.stub("channelId" + i))
                .toList();
        given(subscribeUseCase.listSubscribeChannel(any())).willReturn(list);
        given(userUserCase.getUser(any())).willReturn(UserFixtures.stub());

        mockMvc
                .perform(
                        get("/api/v1/subscribe/mine")
                                .header(HeaderAttribute.X_AUTH_KEY, UUID.randomUUID().toString())
                )
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.size()").value(3)
                );
    }
}