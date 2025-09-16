package com.videoservice.manager;

import com.videoservice.manager.mq.NewVideMessageProducer;
import org.springframework.stereotype.Component;

@Component
public class MessagePersistenceAdapter implements MessagePort {

    private final NewVideMessageProducer newVideMessageProducer;

    public MessagePersistenceAdapter(NewVideMessageProducer newVideMessageProducer) {
        this.newVideMessageProducer = newVideMessageProducer;
    }

    @Override
    public void sendNewVideMessage(String channelId) {
        newVideMessageProducer.newVideMessageRequest(channelId);
    }
}