// 신규 영상 Kafka 메시지 consumer의 멱등 처리를 검증하는 테스트
package com.videoservice.manager.mq;

import static com.videoservice.manager.mq.common.KafkaNames.TOPIC;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.videoservice.manager.jpa.event.ProcessedEventJpaEntity;
import com.videoservice.manager.jpa.event.ProcessedEventJpaRepository;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaEntity;
import com.videoservice.manager.jpa.subscribe.SubscribeJpaRepository;
import com.videoservice.manager.jpa.user.UserJpaEntity;
import com.videoservice.manager.mq.dto.NewVideoMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class NewVideMessageConsumerTest {
    private NewVideMessageConsumer sut;

    private final SubscribeJpaRepository subscribeJpaRepository = mock(SubscribeJpaRepository.class);
    private final ProcessedEventJpaRepository processedEventJpaRepository = mock(ProcessedEventJpaRepository.class);
    private final TestTransactionManager transactionManager = new TestTransactionManager();

    @BeforeEach
    void setUp() {
        sut = new NewVideMessageConsumer(
                subscribeJpaRepository,
                processedEventJpaRepository,
                new TransactionTemplate(transactionManager)
        );
    }

    @Test
    void consumesSameEventOnlyOnceAndAcknowledgesBothDeliveries() {
        Set<String> processedEventIds = new HashSet<>();
        NewVideoMessage message = new NewVideoMessage("eventId", "channelId");
        ConsumerRecord<String, NewVideoMessage> record = new ConsumerRecord<>(TOPIC, 0, 1L, null, message);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        SubscribeJpaEntity subscribe = new SubscribeJpaEntity(
                "subscribeId",
                null,
                new UserJpaEntity("userId", "name", "profileImageUrl")
        );

        given(processedEventJpaRepository.existsById("eventId"))
                .willAnswer(invocation -> processedEventIds.contains(invocation.getArgument(0)));
        given(processedEventJpaRepository.saveAndFlush(any()))
                .willAnswer(invocation -> {
                    ProcessedEventJpaEntity entity = invocation.getArgument(0);
                    processedEventIds.add(entity.getEventId());
                    return entity;
                });
        given(subscribeJpaRepository.findAllByChannelId("channelId")).willReturn(List.of(subscribe));

        sut.consumeCouponIssueRequest(record, acknowledgment);
        sut.consumeCouponIssueRequest(record, acknowledgment);

        verify(subscribeJpaRepository, times(1)).findAllByChannelId("channelId");
        verify(processedEventJpaRepository, times(1)).saveAndFlush(any());
        verify(acknowledgment, times(2)).acknowledge();
        then(transactionManager.commits).isEqualTo(2);
    }

    @Test
    void doesNotAcknowledgeWhenNotificationProcessingFails() {
        RuntimeException exception = new RuntimeException("notification failed");
        NewVideoMessage message = new NewVideoMessage("eventId", "channelId");
        ConsumerRecord<String, NewVideoMessage> record = new ConsumerRecord<>(TOPIC, 0, 1L, null, message);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        given(processedEventJpaRepository.existsById("eventId")).willReturn(false);
        given(processedEventJpaRepository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(subscribeJpaRepository.findAllByChannelId("channelId")).willThrow(exception);

        assertThatThrownBy(() -> sut.consumeCouponIssueRequest(record, acknowledgment))
                .isSameAs(exception);

        verify(processedEventJpaRepository).saveAndFlush(any());
        verify(acknowledgment, never()).acknowledge();
        then(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    void acknowledgesConcurrentDuplicateWithoutNotification() {
        NewVideoMessage message = new NewVideoMessage("eventId", "channelId");
        ConsumerRecord<String, NewVideoMessage> record = new ConsumerRecord<>(TOPIC, 0, 1L, null, message);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        given(processedEventJpaRepository.existsById("eventId")).willReturn(false);
        given(processedEventJpaRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("duplicate eventId"));

        sut.consumeCouponIssueRequest(record, acknowledgment);

        verify(subscribeJpaRepository, never()).findAllByChannelId(any());
        verify(acknowledgment).acknowledge();
        then(transactionManager.rollbacks).isEqualTo(1);
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
