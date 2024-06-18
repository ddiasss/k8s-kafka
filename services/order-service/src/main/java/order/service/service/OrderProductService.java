package order.service.service;


import order.service.domain.OrderProduct;
import order.service.enums.OrderStatus;
import order.service.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;

@Service
public class OrderProductService {

    private final OrderRepository repository;
    private final String kafkaHosts;

    public OrderProductService(OrderRepository repository, @Value("${kafka.hosts}") String kafkaHosts) {
       this.repository = repository;
        this.kafkaHosts = kafkaHosts;
    }

    @Transactional
    public Long create(OrderProduct orderProduct) {
        // Base order
        orderProduct.setStatus(OrderStatus.PENDING);
        repository.saveAndFlush(orderProduct);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHosts);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        Producer<Long, Long> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("orders", orderProduct.getId(), orderProduct.getProductId()));
        producer.close();

        return orderProduct.getId();
    }

    @Transactional(readOnly = true)
    public OrderProduct findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

}
