package com.videoservice.manager;

import static com.videoservice.manager.common.CacheNames.CHANNEL;

import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.channel.ChannelJpaRepository;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
public class ChannelPersistenceAdapter implements LoadChannelPort, SaveChannelPort {
    private final ChannelJpaRepository channelJpaRepository;

    public ChannelPersistenceAdapter(ChannelJpaRepository channelJpaRepository) {
        this.channelJpaRepository = channelJpaRepository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CHANNEL, key = "#channel.id")
    })
    public void saveChannel(Channel channel) {
        channelJpaRepository.save(ChannelJpaEntity.from(channel));
    }

    @Override
    @Cacheable(cacheNames = CHANNEL, key = "#channelId")
    public Optional<Channel> loadChannel(String id) {
        return Optional.of(channelJpaRepository.findById(id)
                .map(ChannelJpaEntity::toDomain)
                .orElseThrow());
    }
}
