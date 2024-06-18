package stock.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stock.service.domain.Stock;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);

}
