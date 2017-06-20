package com.test.kafka.consumer.types;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ConsumerRoute {

    @Autowired
    private ConsumerSingle consumerSingle;

    @Autowired
    private ConsumerMultiple consumerMultiple;

    private static final Logger log = Logger.getLogger(ConsumerRoute.class);

    @PostConstruct
    public void startConsumer() throws InterruptedException {

        long seconds = 10;
        String duration = System.getProperty("duration");
        if (duration != null) {
            seconds = Integer.parseInt(duration);
        }

        String type = System.getProperty("type");
        if (type != null)
            type = type.toUpperCase();
        else
            type = "";

        switch (type) {
            case "AUTO":
                log.info("Starting AUTO-COMMIT Consumer...");
                consumerSingle.autoCommitConsumer(seconds);
                break;
            case "SYNC":
                log.info("Starting SYNC Consumer...");
                consumerSingle.wakeupConsumer(seconds);
                break;
            case "ATLEAST":
                log.info("Starting ATLEAST ONCE Consumer...");
                consumerSingle.atLeastOnceConsumer(seconds);
                break;
            case "ATMOST":
                log.info("Starting ATMOST ONCE Consumer...");
                consumerSingle.atMostOnceConsumer(seconds);
                break;
            case "EXACT":
                log.info("Starting EXACTLY-ONCE (ONE-RECORD) Consumer...");
                consumerSingle.commitOneRecordConsumer(seconds);
                break;
            case "PARTITION":
                log.info("Starting ONE-PARTITION Consumer...");
                consumerSingle.commitOnePartitionConsumer(seconds);
                break;
            case "ASYNC":
                log.info("Starting ASYNC Consumer...");
                consumerSingle.commitAsyncConsumer(seconds);
                break;
            case "MULTI":
                log.info("Starting MULTIPLE Consumer...");
                consumerMultiple.start(seconds);
                break;
            default:
                log.info("Starting DEFAULT(AUTO-COMMIT) Consumer...");
                consumerSingle.autoCommitConsumer(seconds);
                break;
        }
    }

}
