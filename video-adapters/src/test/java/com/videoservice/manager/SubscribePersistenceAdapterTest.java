package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.domain.channel.ChannelFixtures;
import com.videoservice.manager.domain.user.UserFixtures;
import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaRepository;
import com.videoservice.manager.jpa.user.UserJpaEntity;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class SubscribePersistenceAdapterTest {
    private SubscribePersistenceAdapter sut;

    private SubscribeJpaRepository subscribeJpaRepository = mock(SubscribeJpaRepository.class);
    private StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private SetOperations<String, String> setOperations = mock(SetOperations.class);

    @BeforeEach
    void setUp() {
        sut = new SubscribePersistenceAdapter(subscribeJpaRepository, stringRedisTemplate);

        given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
    }

    @Test
    void testInsertSubscribe() {
        var channel = ChannelFixtures.stub("channelId");
        var user = UserFixtures.stub();
        given(subscribeJpaRepository.save(any())).willAnswer(arg -> arg.getArgument(0));

        var result = sut.insertSubscribeChannel(channel, user);

        then(result).isNotNull();
        verify(subscribeJpaRepository).save(any());
    }

    @Test
    void testDeleteSubscribe() {
        var subscribeId = "subscribedId";
        var channel = ChannelFixtures.stub("channelId");
        var user = UserFixtures.stub();
        given(subscribeJpaRepository.findById(any()))
                .willReturn(Optional.of(new SubscribeJpaEntity(subscribeId, ChannelJpaEntity.from(channel), UserJpaEntity.from(user))));

        sut.deleteSubscribeChannel(subscribeId);

        verify(subscribeJpaRepository).deleteById(any());
    }

    @Test
    void testListSubscribeChannel() {
        var list = IntStream.range(1, 4)
                .mapToObj(i -> new SubscribeJpaEntity(
                                String.valueOf(i),
                                ChannelJpaEntity.from(ChannelFixtures.stub(String.valueOf(i))),
                                UserJpaEntity.from(UserFixtures.stub())
                        )
                )
                .toList();

        given(subscribeJpaRepository.findAllByUserId(any()))
                .willReturn(list);

        var result = sut.listSubscribeChannel("userId");

        then(result).hasSize(3);
    }
}