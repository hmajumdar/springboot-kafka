package com.test.kafka.consumer.types;

import com.test.kafka.config.ConsumerConfigFactory;
import com.test.kafka.consumer.service.ProcessingService;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by lgp941 on 8/5/16.
 */
@Component
@EnableAsync
public class ConsumerSingle {

    private static volatile boolean running = true;

    @Autowired
    private ConsumerConfigFactory consumerConfigFactory;

    @Autowired
    private ProcessingService processingService;

    public void autoCommitConsumer(long seconds) {
        Thread flagger = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000 * seconds);
                    running = false;
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        };
        flagger.start();

        try (KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig()) {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                if (records.count() > 0)
                    for (ConsumerRecord<String, String> record : records) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("partition", record.partition());
                        map.put("offset", record.offset());
                        map.put("value", record.value());
                        System.out.println("Processed" + ": " + map);
                    }
                else
                    System.out.println("got nothing");
            }
        }
    }

    public void wakeupConsumer(long seconds) {
        try (KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig()) {
            Thread killer = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000 * seconds);
                        System.out.println("waking up...");
                        consumer.wakeup();
                    } catch (InterruptedException ie) {
                        throw new RuntimeException();
                    }
                }
            };

            killer.start();

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partition", record.partition());
                    map.put("offset", record.offset());
                    map.put("value", record.value());
                    System.out.println("Processed" + ": " + map);
                }
            }
        } catch (WakeupException e) {
            System.out.println("woken");
        }
    }

    public void atLeastOnceConsumer(long seconds) {
        KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig();
        try {

            Thread flagger = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000 * seconds);
                        running = false;
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            };
            flagger.start();

            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partition", record.partition());
                    map.put("offset", record.offset());
                    map.put("value", record.value());
                    System.out.println("Processed" + ": " + map);
                }
                if (records.count() > 0) {
                    try {
                        System.out.println("commit");
                        consumer.commitSync();
                    } catch (CommitFailedException e) {
                        // application specific failure handling
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }

    public void atMostOnceConsumer(long seconds) {
        KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig();
        try {
            Thread flagger = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000 * seconds);
                        running = false;
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            };
            flagger.start();
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                if (records.count() > 0) {
                    try {
                        System.out.println("commit");
                        consumer.commitSync();
                    } catch (CommitFailedException e) {
                        // application specific failure handling
                    }
                }

                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partition", record.partition());
                    map.put("offset", record.offset());
                    map.put("value", record.value());
                    System.out.println("Processed" + ": " + map);
                }
            }
        } finally {
            consumer.close();
        }
    }

    public void commitOneRecordConsumer(long seconds) throws InterruptedException {
        KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig();

        try {
            Thread flagger = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000 * seconds);
                        running = false;
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            };
            flagger.start();
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                try {
                    for (ConsumerRecord<String, String> record : records) {

                        processingService.process(record);
                        // todo: return a future object with the offset that was processed, read the future object to commit the offset.
                        // Retry if the processing failed. If it still fails , note down the offset and call later to process, or send generic error to customers.

                        consumer.commitSync(Collections.singletonMap(new TopicPartition(record.topic(),record.partition()), new OffsetAndMetadata(record.offset() + 1)));

                        System.out.println("Committed Offset" + ": " + record.offset() + " for Partition:" + record.partition());

                    }
                } catch (CommitFailedException e) {
                    // application specific failure handling
                }
            }
        } finally {
            consumer.close();
        }
    }

    public void commitOnePartitionConsumer(long seconds) {
        KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig();
        try {
            Thread flagger = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000 * seconds);
                        running = false;
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            };
            flagger.start();
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, String> record : records) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("partition", record.partition());
                        map.put("offset", record.offset());
                        map.put("value", record.value());
                        System.out.println("Processed" + ": " + map);
                    }

                    long lastoffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    consumer.commitSync(Collections.singletonMap(partition,
                            new OffsetAndMetadata(lastoffset + 1)));
                }
            }
        } finally {
            consumer.close();
        }
    }


    public void commitAsyncConsumer(long seconds) {
        KafkaConsumer<String, String> consumer = consumerConfigFactory.getConsumerConfig();
        try {
            Thread flagger = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000 * seconds);
                        running = false;
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            };
            flagger.start();
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partition", record.partition());
                    map.put("offset", record.offset());
                    map.put("value", record.value());
                    System.out.println("Processed" + ": " + map);
                }
                consumer.commitAsync(new OffsetCommitCallback() {
                    @Override
                    public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                        if (exception != null) {
                            // application specific failure handling
                        }
                    }
                });
            }
        } finally {
            consumer.close();
        }
    }

}
