Table of contents
=================

  * [Rebalancing Test for Kafka 0.10 API](#rebalancing-test-for-kafka-0.10-api)
  * [Infrastructure Setup Commands](#infrastructure-setup-commands)
  * [About this Microservice](#about-this-microservice)
  * [Running](#running)
  * [Test Cases](#test-cases)
  * [Issues with Zeno Applications on Kafka V8](#issues-with-zeno-applications-on-kafka-v8)


Rebalancing Test for Kafka 0.10 API
===================================

The event in which partition ownership is moved from one consumer to another is called a **rebalance**. Rebalances are usually triggered when a “consumer” goes down or when its added, but ther term "rebalancing" is also often used
to describe when broker/node goes down or is added. So, there are two things that people usually mean when they talk about rebalancing.

1. Leader re-election, or preferred replica election: When Broker gets added/deleted
2. Partition rebalancing: When Consumer gets added/deleted

Thru this test project we would perform Rebalancing Tests and find the best approach to address issues faced by applications onboarded to Zeno.


## Infrastructure Setup Test Cases

| Brokers        |  Topics      |   Partitions  |     Consumer-Groups   |   Consumers   |
| ------------- |:-------------:|:-------------:|:---------------------:|:-------------:|
| 1             |       1       |       1       |           1           |       1       |
| 1             |       1       |       2       |           1           |       3       |
| 2             |       1       |       4       |           2           |       6       |
| 5             |       1       |       2       |           2           |       6       |



Infrastructure Setup Commands
=============================

**PWD**
`/Users/lgp941/Work/installations/kafka_2.11-0.10.0.0`

**START ZOOKEEPER**
`bin/zookeeper-server-start.sh config/zookeeper.properties &`

**START SERVER**
`bin/kafka-server-start.sh config/server.properties &`

**CREATE TOPICS**
`bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test2`

**LIST TOPICS**
`bin/kafka-topics.sh --list --zookeeper localhost:2181`

**NOTE**
For setting up multiple brokers on a single node, different server property  files are required for each broker.
Each property  file will define unique, different values for the following properties

* broker.id
* port
* log.dir

For example, in server-1.properties used for broker1, we define the following:

* broker.id=1
* port=9093
* log.dir=/tmp/kafka-logs-1



About this Microservice
=======================

This is a spring-boot microservice project, which currently supports the following types of Kafka consumers which were added in Kafka v10.

1. AutoCommit
2. Synchronous Consumer
3. Atleast Once Consumer
4. Atmost Once Consumer
5. Exactly Once Consumer (One record at a time)
6. Commit to a specific partition consumer
7. Asynchronous Consumer
8. Multiple Consumer


Running
=================

* Running the microservice with a default consumer "AutoCommit"

    `mvn spring-boot:run`

* Running this microservice with a specific consumer type

    `mvn spring-boot:run -Dtype="atleast"`

* Running this microservice with a specific consumer type and duration (i.e for how long you want to run this consumer microservice) in seconds.

    `mvn spring-boot:run -Dtype="synch" -Dtopic="topicName" -Dduration="40"`

**type** = "auto", "synch", "atleast", "atmost", "exact", "partition", "async", "multiple"


Test Cases
==========

**Test-1:** Pristine Kafka Topic

**Description:** Analyze Messages when new consumer subscribes to a topic with only existing messages.

**Test Steps:**

1. Create new topic
2. Produce messages to the topic
3. Start the consumer

---------------------------------------------

**Test-2:** Pristine Kafka Topic with existing and new messages

**Description:** Analyze Messages when new consumer subscribes to a topic with existing messages and then new messages.

**Test Steps:**

1. Create new topic
2. Produce messages to the topic
3. Start the consumer
4. Produce messages to the topic with consumer listening

---------------------------------------------

**Test-3:** Consumer Failure: Pristine Kafka Topic with existing, new messages and restarting the consumer

**Description:** Analyze Messages when new consumer subscribes to a topic is restarted with existing messages and new messages.

**Test Steps:**

1. Create new topic
2. Produce messages to the topic
3. Start the consumer
4. Produce messages to the topic with consumer listening
5. Kill the consumer
6. Produce more messages to the topic
7. Restart the consumer

---------------------------------------------

**Test-4:** Consumer Failure: Message flow with multiple brokers and multiple partitions

**Description:** Analyze Consumer failure when there are mutliple brokers, topic replica and partitions.

**Test Steps:**

1. Start Broker-1
2. Start Broker-2
3. Create new topic
4. Start the consumer
5. Produce messages to the topic
6. Stop the consumer
7. Produce more messages
8. Start consumer

---------------------------------------------
**Test-5:** Broker Failure: Message flow with multiple brokers and multiple partitions

**Description:** Analyze Broker failure when there are mutliple brokers, topic replica and partitions.

**Test Steps:**

1. Start Broker-1
2. Start Broker-2
3. Create new topic
4. Start the consumer
5. Produce messages to the topic
6. Stop Broker-1
7. Produce more messages

---------------------------------------------

**Test-6:** Message commit before processing

**Description:** Analyze Messages when a new message arrives and getting ready to be processed, while an existing message is processed and committed.

**Test Steps:**

1. Start Brokers
2. Start Consumers
3. Produce messages to an existing topic
4. Verify that no message is commited before processing.

## Conclusion
Kafka does not provide exactly-once processing out-of-the box.
You can either have at-least-once processing if you commit messages after you successfully processed them, or you can have at-most-once processing if you commit messages directly after poll() before you start processing.

1. Consumer method **poll()** returns a huge batch and you want to commit offsets in the middle of the batch to avoid having to process all those rows again if a rebalance occurs, then the consumer API allows you to call commitSync() and commitAsync() and pass a map of partitions and offsets that you wish to commit.
This will not guarantee exactly once, but will reduce message loss or duplication in case of failures.

2. Kafka API also lets you seek to a specific offset. This ability can be used in a variety of ways, for example to go back few messages or skip ahead few messages. This might be helpful to process and commit in one atomic operation i.e in a single transaction, however this will need custom implementation which can get fairly complex.

**From Kafka dev forum:** In general, it is not possible for any system to guarantee exactly once semantics because those semantics rely on the source and destination systems coordinating -- the source provides some sort of retry semantics, and the destination system needs to do some sort of deduplication or similar to only "deliver" the data one time.


## Issues with Zeno Applications on Kafka V8
1. **Generic Commit:** Offset is committed as soon as new message arrives before being processed. This usually happens when there is an existing message being processed and on its way to commit. The new message gets committed along with the existing message.
2. **New Consumer:** When a new consumer gets added it does not read existing messages in a Topic, only new message that come into the topic are read by the consumer. This
