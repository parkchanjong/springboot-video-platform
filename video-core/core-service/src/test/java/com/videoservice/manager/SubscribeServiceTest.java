package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.videoservice.manager.domain.channel.ChannelFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscribeServiceTest {
    private SubscribeService sut;

    private SubscribePort subscribePort = mock(SubscribePort.class);
    private LoadChannelPort loadChannelPort = mock(LoadChannelPort.class);
    private LoadUserPort loadUserPort = mock(LoadUserPort.class);

    @BeforeEach
    void setUp() {
        sut = new SubscribeService(subscribePort, loadChannelPort, loadUserPort);
    }

    @Test
    void testSubscribeChannel() {
        var subscribeId = UUID.randomUUID().toString();
        given(loadChannelPort.loadChannel(any())).willReturn(Optional.of(ChannelFixtures.stub("channelId")));
        given(loadUserPort.loadUser(any())).willReturn(Optional.of(UserFixtures.stub()));
        given(subscribePort.insertSubscribeChannel(any(), any())).willReturn(subscribeId);

        var result = sut.subscribeChannel("channelId", "userId");

        then(result).isEqualTo(subscribeId);
    }

    @Test
    void testListSubscribeChannelByUser() {
        var list = IntStream.range(1, 4)
                .mapToObj(i -> ChannelFixtures.stub("channelId" + i))
                .toList();
        given(subscribePort.listSubscribeChannel(any())).willReturn(list);

        var result = sut.listSubscribeChannel("userId");

        then(result)
                .hasSize(3);
    }
}