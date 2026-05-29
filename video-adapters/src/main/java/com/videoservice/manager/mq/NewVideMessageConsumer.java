package com.videoservice.manager.mq;

import static com.videoservice.manager.mq.common.KafkaNames.TOPIC;
import static com.videoservice.manager.mq.common.KafkaNames.GROUP_ID;

import com.videoservice.manager.jpa.event.ProcessedEventJpaEntity;
import com.videoservice.manager.jpa.event.ProcessedEventJpaRepository;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaRepository;
import com.videoservice.manager.mq.dto.NewVideoMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component
public class NewVideMessageConsumer {

    private final SubscribeJpaRepository subscribeJpaRepository;
    private final ProcessedEventJpaRepository processedEventJpaRepository;
    private final TransactionTemplate transactionTemplate;

    public NewVideMessageConsumer(
            SubscribeJpaRepository subscribeJpaRepository,
            ProcessedEventJpaRepository processedEventJpaRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.subscribeJpaRepository = subscribeJpaRepository;
        this.processedEventJpaRepository = processedEventJpaRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
    public void consumeCouponIssueRequest(
            ConsumerRecord<String, NewVideoMessage> record,
            Acknowledgment acknowledgment
    ) {
        NewVideoMessage message = record.value();
        validateEventId(message);

        try {
            transactionTemplate.executeWithoutResult(status -> processMessage(record, message));
            acknowledgment.acknowledge();
        } catch (DuplicateProcessedEventException e) {
            acknowledgment.acknowledge();
        }
    }

    private void processMessage(ConsumerRecord<String, NewVideoMessage> record, NewVideoMessage message) {
        if (processedEventJpaRepository.existsById(message.eventId())) {
            return;
        }

        claimProcessedEvent(record, message);

        String channelId = message.channelId();

        subscribeJpaRepository.findAllByChannelId(channelId).stream()
                .map(SubscribeJpaEntity::getUser)
                .forEach(user -> System.out.println( user.getId() + "," + channelId + " 채널에 새로운 영상이 등록되었습니다."));
    }

    private void claimProcessedEvent(ConsumerRecord<String, NewVideoMessage> record, NewVideoMessage message) {
        try {
            processedEventJpaRepository.saveAndFlush(ProcessedEventJpaEntity.of(
                    message.eventId(),
                    record.topic(),
                    record.partition(),
                    record.offset()
            ));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateProcessedEventException(e);
        }
    }

    private void validateEventId(NewVideoMessage message) {
        if (message == null || !StringUtils.hasText(message.eventId())) {
            throw new IllegalArgumentException("eventId is required");
        }
    }

    private static class DuplicateProcessedEventException extends RuntimeException {
        private DuplicateProcessedEventException(Throwable cause) {
            super(cause);
        }
    }
}
