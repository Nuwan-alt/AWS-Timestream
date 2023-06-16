package org.ohlcTS.kafka;


import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class KafkaUtils {
    @Getter
    @Setter
    private KafkaProcessor processor;
    private KafkaConfig config;
    private KafkaConsumer consumer;
    private HashMap<String, HashMap<Integer, Long>> lastFailedOffsetMap;

    public KafkaUtils(List<String> topics) throws FileNotFoundException {
        this.config = new KafkaConfig();
        this.consumer = config.getConsumer(topics);
        this.lastFailedOffsetMap = new HashMap<>();
//        System.out.println(consumer.listTopics());
    }

    public void pollRecords() {
        ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));

        ConsumerRecord<String, String> currentRecord = null;

        try {
            if (!consumerRecords.isEmpty()) {
                Optional<KafkaProcessor> processor = Optional.ofNullable(getProcessor());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    currentRecord = record;

                    if (processor.isPresent()) {
                        processor.get().processRecords(record.value(), record.topic());
                    }
                }
                consumer.commitAsync();
            }
        } catch (KafkaException ke) {


            HashMap<String, HashMap<Integer, TopicPartition>> topicPartitionMap = KafkaConfig.topicPartitionMap;
            HashMap<Integer, TopicPartition> partitionMap = topicPartitionMap.get(currentRecord.topic());
            TopicPartition partition = partitionMap.get(currentRecord.partition());
            long currentOffset = currentRecord.offset();

            if (!lastFailedOffsetMap.containsKey(currentRecord.topic())) {
                lastFailedOffsetMap.put(currentRecord.topic(), new HashMap<>());
            }

            HashMap<Integer, Long> failedOffsetForTopicMap = lastFailedOffsetMap.get(currentRecord.topic());

            if (!failedOffsetForTopicMap.containsKey(currentRecord.partition())) {
                failedOffsetForTopicMap.put(currentRecord.partition(), 0L);
            }
            long failedOffset = failedOffsetForTopicMap.get(currentRecord.partition());

            if (failedOffset != currentOffset) {
                failedOffsetForTopicMap.put(currentRecord.partition(), currentOffset);
                consumer.seek(partition, currentOffset);
            } else {
                consumer.seek(partition, currentOffset + 1);
            }
        } catch (Exception e) {
            System.out.println(e);
//            LogManager.serverLogger.error("<KafkaUtil> Fail to process record ", e);

        }
    }
}
