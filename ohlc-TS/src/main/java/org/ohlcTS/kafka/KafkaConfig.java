package org.ohlcTS.kafka;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ohlcTS.utils.Setting;

import java.io.FileNotFoundException;
import java.util.*;

public class KafkaConfig {
    private Map<String,String> Settings;

    public KafkaConfig() throws FileNotFoundException {
        Setting setting = new Setting();
        this.Settings = setting.getSettings();
    }
    public static HashMap<String, HashMap<Integer, TopicPartition>> topicPartitionMap = new HashMap();

    public KafkaConsumer<String, String> getConsumer(List<String> topics) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Settings.get("KAFKA_BROKERS"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, Settings.get("KAFKA_GROUP_ID_CONFIG"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Settings.get("KAFKA_MAX_POLL_RECORDS"));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, Settings.get("KAFKA_OFFSET_RESET_EARLIER"));

        KafkaConsumer consumer = new KafkaConsumer<>(props);
        consumer.subscribe(topics);

        if(Boolean.parseBoolean(Settings.get("KAFKA_SEQUENCE_START"))){
            setConsumerRebalancer(topics, consumer, -1, false);
        } else {
            setConsumerRebalancer(topics, consumer, Integer.MAX_VALUE, true);
        }
        return consumer;
    }

    public void setConsumerRebalancer(List<String> topics, KafkaConsumer consumer, long startOffset, boolean resume) {
        consumer.subscribe(topics, new ConsumerRebalanceListener() {
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.printf("%s topic-partitions are revoked from this consumer\n", Arrays.toString(partitions.toArray()));
            }

            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.printf("%s topic-partitions are assigned to this consumer\n", Arrays.toString(partitions.toArray()));

                Iterator<TopicPartition> topicPartitionIterator = partitions.iterator();
                while (topicPartitionIterator.hasNext()) {
                    TopicPartition topicPartition = topicPartitionIterator.next();

                    if(!topicPartitionMap.containsKey(topicPartition.topic())) {
                        topicPartitionMap.put(topicPartition.topic(), new HashMap<>());
                    }

                    HashMap<Integer, TopicPartition> partitionMap = topicPartitionMap.get(topicPartition.topic());
                    partitionMap.put(topicPartition.partition(), topicPartition);

                    long position = consumer.position(topicPartition);
                    Optional<OffsetAndMetadata> offset = Optional.ofNullable(consumer.committed(topicPartition));
                    System.out.println("Current offset is " + position + " committed offset is ->" + offset.orElse(null));

                    if (startOffset == 0) {
                        System.out.println("Setting offset to beginning");
                        consumer.seekToBeginning(Arrays.asList(topicPartition));
                    } else if (startOffset == -1) {
                        System.out.println("Setting it to the end ");
                        consumer.seekToEnd(Arrays.asList(topicPartition));
                    } else {
                        long start = startOffset;

                        if (offset.isPresent() && offset.get().offset() <= position && resume) {
                            start = offset.get().offset();
                            consumer.seek(topicPartition, start);
                        } else if (startOffset < position) {
                            start = startOffset;
                            consumer.seek(topicPartition, start);
                        } else {
                            consumer.seekToBeginning(Arrays.asList(topicPartition));
                            start = consumer.position(topicPartition);
                        }

                        System.out.println("Resetting offset to " + start);
                    }
                }
            }
        });
    }
}
