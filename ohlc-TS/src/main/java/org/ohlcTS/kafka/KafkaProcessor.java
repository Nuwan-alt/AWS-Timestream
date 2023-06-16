package org.ohlcTS.kafka;

public interface KafkaProcessor {
    void processRecords(String record, String topic) throws KafkaException;
}
