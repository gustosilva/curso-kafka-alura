package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

public class KafkaService implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaService.class.getName());
    private final KafkaConsumer<String, String> consumer;
    private final ConsumerFunction parse;

    public KafkaService(String groupId, String topic, ConsumerFunction parse) {
        this.parse = parse;
        this.consumer = new KafkaConsumer<>(properties(groupId));
        this.consumer.subscribe(Collections.singletonList(topic));
    }

    public KafkaService(String groupId, Pattern topics, ConsumerFunction parse) {
        this.parse = parse;
        this.consumer = new KafkaConsumer<>(properties(groupId));
        this.consumer.subscribe(topics);
    }

    public void run() {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            if (!records.isEmpty()) {
                LOGGER.warn("Foram encontrados " + records.count() + " registros");
            }

            records.forEach(consumerRecord -> parse.consume(consumerRecord));
        }
    }

    private static Properties properties(String groupId) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        return properties;
    }

    @Override
    public void close() {
        consumer.close();
    }
}
