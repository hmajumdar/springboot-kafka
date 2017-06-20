package com.test.kafka.config;

import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

@Component
public class ConsumerConfigFactory {

    private KafkaConsumer<String, String> consumer;
    private static final String TOPIC = "pristine";

    @PostConstruct
    private void createConsumerConfig() {
        Properties properties = new Properties();
        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            properties.load(props);
        }catch (Throwable throwable) {
            System.out.printf("%s", throwable.getStackTrace());
        }
        this.consumer = new KafkaConsumer<>(properties);

        String topic = System.getProperty("topic");
        if (topic != null)
            consumer.subscribe(Arrays.asList(topic));
        else
            consumer.subscribe(Arrays.asList(TOPIC));
    }

    public KafkaConsumer<String,String> getConsumerConfig() {
        return consumer;
    }
}