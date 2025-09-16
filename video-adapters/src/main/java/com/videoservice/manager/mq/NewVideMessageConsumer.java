package com.videoservice.manager.mq;

import static com.videoservice.manager.mq.common.KafkaNames.TOPIC;
import static com.videoservice.manager.mq.common.KafkaNames.GROUP_ID;

import com.videoservice.manager.jpa.subscribe.SubscribeJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaRepository;
import com.videoservice.manager.mq.dto.NewVideoMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NewVideMessageConsumer {

    private final SubscribeJpaRepository subscribeJpaRepository;

    public NewVideMessageConsumer(SubscribeJpaRepository subscribeJpaRepository) {
        this.subscribeJpaRepository = subscribeJpaRepository;
    }

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
    public void consumeCouponIssueRequest(NewVideoMessage message) {

        String channelId = message.channelId();

        subscribeJpaRepository.findAllByChannelId(channelId).stream()
                .map(SubscribeJpaEntity::getUser)
                .forEach(user -> System.out.println( user.getId() + "," + channelId + " 채널에 새로운 영상이 등록되었습니다."));
    }
}
