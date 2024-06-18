package stock.service.consumer;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import stock.service.domain.Stock;
import stock.service.enums.OrderStatus;
import stock.service.repository.StockRepository;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
public class OrderConsumer {

    private final StockRepository repository;
    private final String kafkaHosts;

    public OrderConsumer(StockRepository repository, @Value("${kafka.hosts}") String kafkaHosts) {
        this.repository = repository;
        this.kafkaHosts = kafkaHosts;
    }

    @PostConstruct
    private void init() {
        new Thread(this::consume).start();
    }

    private void consume() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHosts);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "base-order-consumer");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        KafkaConsumer<Long, Long> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("orders"));
        while (true) {
            ConsumerRecords<Long, Long> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<Long, Long> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s %n", record.offset(), record.key(), record.value());

                final Long orderId = record.key();
                final Long productOrderId = record.value();

                Stock stock = repository.findByProductId(productOrderId).orElseThrow();

                OrderStatus status = OrderStatus.FAILED;

                if (stock.getAmount() > 0) {
                    status = OrderStatus.SUCCESS;
                    // This isn't a secure way do it, just a sample
                    stock.setAmount(stock.getAmount() - 1);
                    repository.saveAndFlush(stock);
                }

                Properties producerProps = new Properties();
                producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHosts);
                producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
                producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

                Producer<Long, String> producer = new KafkaProducer<>(producerProps);
                producer.send(new ProducerRecord<>("orders-status", orderId, status.toString()));
                producer.close();
            }
        }
    }

}
