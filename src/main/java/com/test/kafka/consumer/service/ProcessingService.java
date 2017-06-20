package com.test.kafka.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lgp941 on 8/9/16.
 */
@Service
public class ProcessingService {

    @Async
    public void process(ConsumerRecord<String, String> record) throws InterruptedException {
        Thread.sleep(5000L);
        Map<String, Object> map = new HashMap<>();
        map.put("partition", record.partition());
        map.put("offset", record.offset());
        map.put("value", record.value());
        System.out.println("Processed" + ": " + map);
    }

}