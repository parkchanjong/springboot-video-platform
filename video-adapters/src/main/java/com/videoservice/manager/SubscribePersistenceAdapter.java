package com.videoservice.manager;

import static com.videoservice.manager.common.CacheNames.SUBSCRIBE_CHANNEL_BY_USER;
import static com.videoservice.manager.common.RedisKeyGenerator.getSubscribeChannelKey;
import static com.videoservice.manager.common.RedisKeyGenerator.getSubscribeUserKey;

import com.videoservice.manager.channel.Channel;
import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaRepository;
import com.videoservice.manager.jpa.user.UserJpaEntity;
import com.videoservice.manager.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubscribePersistenceAdapter implements SubscribePort {
    private final SubscribeJpaRepository subscribeJpaRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public SubscribePersistenceAdapter(SubscribeJpaRepository subscribeJpaRepository, StringRedisTemplate stringRedisTemplate) {
        this.subscribeJpaRepository = subscribeJpaRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @CacheEvict(cacheManager = "redisTtl10mCacheManager", cacheNames = SUBSCRIBE_CHANNEL_BY_USER, key = "#user.id")
    public String insertSubscribeChannel(Channel channel, User user) {
        var subscribeJpaEntity = new SubscribeJpaEntity(
                UUID.randomUUID().toString(),
                ChannelJpaEntity.from(channel),
                UserJpaEntity.from(user)
        );
        subscribeJpaRepository.save(subscribeJpaEntity);

        var setOps = stringRedisTemplate.opsForSet();
        setOps.add(getSubscribeChannelKey(channel.getId()), user.getId());
        setOps.add(getSubscribeUserKey(user.getId()), channel.getId());

        return subscribeJpaEntity.getId();
    }

    @Override
    @CacheEvict(cacheManager = "redisTtl10mCacheManager", cacheNames = SUBSCRIBE_CHANNEL_BY_USER, key = "#userId")
    public void deleteSubscribeChannel(String subscribeId) {
        var subscribeJpaEntity = subscribeJpaRepository.findById(subscribeId).get();
        var channelId = subscribeJpaEntity.getChannel().getId();
        var userId = subscribeJpaEntity.getUser().getId();

        var setOps = stringRedisTemplate.opsForSet();
        setOps.remove(getSubscribeChannelKey(channelId), userId);
        setOps.remove(getSubscribeUserKey(userId), channelId);

        subscribeJpaRepository.deleteById(subscribeId);
    }

    @Override
    @Cacheable(cacheManager = "redisTtl10mCacheManager", cacheNames = SUBSCRIBE_CHANNEL_BY_USER, key = "#userId")
    public List<Channel> listSubscribeChannel(String userId) {
        return subscribeJpaRepository.findAllByUserId(userId)
                .stream()
                .map(SubscribeJpaEntity::getChannel)
                .map(ChannelJpaEntity::toDomain)
                .toList();
    }
}
