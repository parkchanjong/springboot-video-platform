package com.videoservice.manager.mq;

import static com.videoservice.manager.mq.common.KafkaNames.TOPIC;

import com.videoservice.manager.mq.dto.NewVideoMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NewVideMessageProducer {
    private final KafkaTemplate<String, NewVideoMessage> kafkaTemplate;

    public NewVideMessageProducer(KafkaTemplate<String, NewVideoMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void newVideMessageRequest(String channelId) {
        kafkaTemplate.send(TOPIC, new NewVideoMessage(channelId));
    }
}
