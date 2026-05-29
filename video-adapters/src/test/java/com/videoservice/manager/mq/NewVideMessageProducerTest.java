// 신규 영상 Kafka 메시지 producer의 eventId 생성을 검증하는 테스트
package com.videoservice.manager.mq;

import static com.videoservice.manager.mq.common.KafkaNames.TOPIC;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.mq.dto.NewVideoMessage;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class NewVideMessageProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, NewVideoMessage> kafkaTemplate = mock(KafkaTemplate.class);
    private final NewVideMessageProducer sut = new NewVideMessageProducer(kafkaTemplate);

    @Test
    void sendsMessageWithGeneratedEventId() {
        ArgumentCaptor<NewVideoMessage> messageCaptor = ArgumentCaptor.forClass(NewVideoMessage.class);

        sut.newVideMessageRequest("channelId");

        verify(kafkaTemplate).send(org.mockito.Mockito.eq(TOPIC), messageCaptor.capture());
        NewVideoMessage message = messageCaptor.getValue();
        then(message.channelId()).isEqualTo("channelId");
        then(message.eventId()).isNotBlank();
        then(UUID.fromString(message.eventId())).isNotNull();
    }
}
