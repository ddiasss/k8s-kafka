package order.service.controller;

import order.service.domain.OrderProduct;
import order.service.service.OrderProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
public class OrderProductController {

    private final OrderProductService service;

    public OrderProductController(OrderProductService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody OrderProduct orderProduct) {
        return ResponseEntity.ok(service.create(orderProduct));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderProduct> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

}
