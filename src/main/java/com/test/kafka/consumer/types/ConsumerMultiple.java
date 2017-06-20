package com.test.kafka.consumer.types;

/**
 * Created by lgp941 on 8/7/16.
 */

import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ConsumerMultiple implements Runnable {

    private KafkaConsumer<String, String> consumer;
    private List<String> topics;
    private int id;
    public String groupId;

    public ConsumerMultiple() {
    }

    public ConsumerMultiple(int id, String groupId, List<String> topics) {
        this.id = id;
        this.topics = topics;
        this.groupId = groupId;

        Properties properties = new Properties();
        try (InputStream props = Resources.getResource("consumer-multi.props").openStream()) {
            properties.load(props);
        }catch (Throwable throwable) {
            System.out.printf("%s", throwable.getStackTrace());
        }
        this.consumer = new KafkaConsumer<>(properties);

    }

    @Override
    public void run() {
        try {
            consumer.subscribe(topics);
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partition", record.partition());
                    map.put("offset", record.offset());
                    map.put("value", record.value());
                    System.out.println(this.id + ": " + map);
                }
            }
        } catch (WakeupException e) {
            // ignore for shutdown
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }

    public void start(long seconds) {
        int numConsumers = 3;
        String groupId = "multi-group";
        List<String> topics = Arrays.asList("apiTopic");
        ExecutorService executor = Executors.newFixedThreadPool(numConsumers);

        final List<ConsumerMultiple> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            ConsumerMultiple consumer = new ConsumerMultiple(i, groupId, topics);
            consumers.add(consumer);
            executor.submit(consumer);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (ConsumerMultiple consumer : consumers) {
                    consumer.shutdown();
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}