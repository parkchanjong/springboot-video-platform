package com.videoservice.manager;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.domain.channel.ChannelFixtures;
import com.videoservice.manager.jpa.ChannelJpaEntityFixtures;
import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.channel.ChannelJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChannelPersistenceAdapterTest {
    private ChannelPersistenceAdapter sut;

    private final ChannelJpaRepository channelJpaRepository = mock(ChannelJpaRepository.class);

    @BeforeEach
    void setUp() {
        sut = new ChannelPersistenceAdapter(channelJpaRepository);
    }

    @Nested
    @DisplayName("saveChannel")
    class SaveChannelTest {
        @Test
        @DisplayName("Channel 을 Jpa Repository 에 저장")
        void saveChannel() {
            // Given
            var channel = ChannelFixtures.stub("channelId");

            // When
            sut.saveChannel(channel);

            // Then
            var argumentCaptor = ArgumentCaptor.forClass(ChannelJpaEntity.class);
            verify(channelJpaRepository).save(argumentCaptor.capture());
            then(argumentCaptor.getValue())
                    .hasFieldOrPropertyWithValue("id", channel.getId())
                    .hasFieldOrPropertyWithValue("snippet.title", channel.getSnippet().getTitle());
        }
    }

    @Nested
    @DisplayName("loadChannel")
    class LoadChannelTest {
        @Test
        void existVideo_then_returnVideo() {
            // Given
            var channelJpaEntity = ChannelJpaEntityFixtures.stub("channel1");
            given(channelJpaRepository.findById(any()))
                    .willReturn(Optional.of(channelJpaEntity));

            // When
            var result = sut.loadChannel("channel1");

            // Then
            then(result)
                    .isPresent()
                    .hasValueSatisfying(channel -> {
                        then(channel.getId()).isEqualTo("id");
                    });
        }
    }
}