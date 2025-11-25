package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:order_service_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.config.import=optional:file:./",
        "SPRING_CONFIG_IMPORT=optional:file:./",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "server.servlet.context-path="
})
class OrderServiceVerticalIntegrationTest {

    private static final LocalDateTime EXISTING_ORDER_DATE = LocalDateTime.of(2024, 4, 10, 9, 30, 0, 123_000_000);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    private Cart persistedCart;
    private Order persistedOrder;

    @BeforeEach
    void setUp() {
        this.orderRepository.deleteAll();
        this.cartRepository.deleteAll();

        this.persistedCart = this.cartRepository.save(Cart.builder()
                .userId(555)
                .build());

        this.persistedOrder = this.orderRepository.save(Order.builder()
                .orderDate(EXISTING_ORDER_DATE)
                .orderDesc("existing-order")
                .orderFee(120.75)
                .cart(this.persistedCart)
                .build());
    }

    @Test
    void findAllShouldReturnPersistedOrders() {
        List<OrderDto> orders = this.orderService.findAll();

        assertThat(orders).hasSize(1);
        OrderDto singleOrder = orders.get(0);
        assertThat(singleOrder.getOrderId()).isEqualTo(this.persistedOrder.getOrderId());
        assertThat(singleOrder.getOrderDesc()).isEqualTo("existing-order");
        assertThat(singleOrder.getOrderFee()).isEqualTo(120.75);
        assertThat(singleOrder.getCartDto().getCartId()).isEqualTo(this.persistedCart.getCartId());
    }

    @Test
    void findByIdShouldReturnOrderWhenPresent() {
        OrderDto orderDto = this.orderService.findById(this.persistedOrder.getOrderId());

        assertThat(orderDto.getOrderId()).isEqualTo(this.persistedOrder.getOrderId());
        assertThat(orderDto.getOrderDesc()).isEqualTo(this.persistedOrder.getOrderDesc());
        assertThat(orderDto.getCartDto().getCartId()).isEqualTo(this.persistedCart.getCartId());
    }

    @Test
    void saveShouldPersistAndReturnOrder() {
        OrderDto payload = OrderDto.builder()
                .orderDate(EXISTING_ORDER_DATE.plusDays(1))
                .orderDesc("new-order")
                .orderFee(87.40)
                .cartDto(CartDto.builder().cartId(this.persistedCart.getCartId()).build())
                .build();

        OrderDto saved = this.orderService.save(payload);

        assertThat(saved.getOrderId()).isNotNull();
        assertThat(saved.getOrderDesc()).isEqualTo("new-order");
        Optional<Order> persisted = this.orderRepository.findById(saved.getOrderId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getOrderDesc()).isEqualTo("new-order");
    }

    @Test
    void updateShouldModifyExistingOrder() {
        OrderDto payload = OrderDto.builder()
                .orderId(this.persistedOrder.getOrderId())
                .orderDate(this.persistedOrder.getOrderDate().plusHours(2))
                .orderDesc("updated-order")
                .orderFee(150.00)
                .cartDto(CartDto.builder().cartId(this.persistedCart.getCartId()).build())
                .build();

        OrderDto updated = this.orderService.update(payload);

        assertThat(updated.getOrderId()).isEqualTo(this.persistedOrder.getOrderId());
        assertThat(updated.getOrderDesc()).isEqualTo("updated-order");
    Order reloaded = this.orderRepository.findById(this.persistedOrder.getOrderId()).orElseThrow();
    assertThat(reloaded.getOrderDesc()).isEqualTo("updated-order");
    assertThat(reloaded.getOrderFee()).isEqualTo(150.00);
    }

    @Test
    void updateByIdShouldOverrideIdentifier() {
        OrderDto payload = OrderDto.builder()
                .orderDate(EXISTING_ORDER_DATE.plusDays(3))
                .orderDesc("patched-order")
                .orderFee(175.25)
                .cartDto(CartDto.builder().cartId(this.persistedCart.getCartId()).build())
                .build();

        OrderDto updated = this.orderService.update(this.persistedOrder.getOrderId(), payload);

        assertThat(updated.getOrderId()).isEqualTo(this.persistedOrder.getOrderId());
        assertThat(updated.getOrderDesc()).isEqualTo("patched-order");
    Order patched = this.orderRepository.findById(this.persistedOrder.getOrderId()).orElseThrow();
    assertThat(patched.getOrderDesc()).isEqualTo("patched-order");
    assertThat(patched.getOrderFee()).isEqualTo(175.25);
    }

    @Test
    void deleteByIdShouldRemoveOrder() {
        this.orderService.deleteById(this.persistedOrder.getOrderId());

        assertThat(this.orderRepository.findById(this.persistedOrder.getOrderId())).isEmpty();
    }
}
