package order.service.consumer;

import jakarta.annotation.PostConstruct;
import order.service.domain.OrderProduct;
import order.service.enums.OrderStatus;
import order.service.repository.OrderRepository;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
public class OrderStatusConsumer {

    private final OrderRepository orderRepository;
    private final String kafkaHosts;

    public OrderStatusConsumer(OrderRepository orderRepository, @Value("${kafka.hosts}") String kafkaHosts) {
        this.orderRepository = orderRepository;
        this.kafkaHosts = kafkaHosts;
    }

    @PostConstruct
    private void init() {
        new Thread(this::consume).start();
    }

    private void consume() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHosts);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "base-order-status-consumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaConsumer<Long, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("orders-status"));
        while (true) {
            ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<Long, String> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s %n", record.offset(), record.key(), record.value());
                OrderProduct orderProduct = orderRepository.findById(record.key()).orElseThrow();
                orderProduct.setStatus(OrderStatus.valueOf(record.value()));
                orderRepository.saveAndFlush(orderProduct);
            }
        }
    }

}