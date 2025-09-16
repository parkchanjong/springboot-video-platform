package com.videoservice.manager.mq.config;

import static com.videoservice.manager.mq.common.KafkaNames.GROUP_ID;

import com.videoservice.manager.mq.dto.NewVideoMessage;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    @Bean
    public ConsumerFactory<String, NewVideoMessage> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<NewVideoMessage> jsonDeserializer = new JsonDeserializer<>(Object.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NewVideoMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NewVideoMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        factory.setConcurrency(3);
        return factory;
    }
}
