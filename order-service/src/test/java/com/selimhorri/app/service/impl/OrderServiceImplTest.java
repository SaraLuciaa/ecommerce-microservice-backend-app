package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2023, 1, 1, 12, 0);

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        this.orderService = new OrderServiceImpl(this.orderRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        Order order = buildOrder(1, 200, "order-desc", 99.99);
        when(this.orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderDto> result = this.orderService.findAll();

        assertThat(result).hasSize(1);
        OrderDto dto = result.get(0);
        assertThat(dto.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(dto.getOrderDesc()).isEqualTo(order.getOrderDesc());
        assertThat(dto.getOrderFee()).isEqualTo(order.getOrderFee());
        assertThat(dto.getCartDto().getCartId()).isEqualTo(order.getCart().getCartId());
        verify(this.orderRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenOrderExists() {
        Order order = buildOrder(7, 305, "found-order", 150.0);
        when(this.orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));

        OrderDto result = this.orderService.findById(order.getOrderId());

        assertThat(result.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(result.getOrderDesc()).isEqualTo(order.getOrderDesc());
        assertThat(result.getCartDto().getCartId()).isEqualTo(order.getCart().getCartId());
        verify(this.orderRepository).findById(order.getOrderId());
    }

    @Test
    void findByIdShouldThrowWhenOrderMissing() {
        when(this.orderRepository.findById(55)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> this.orderService.findById(55));
        verify(this.orderRepository).findById(55);
    }

    @Test
    void saveShouldPersistAndReturnMappedOrder() {
        OrderDto payload = buildOrderDto(null, 400, "new-order", 210.5);
        Order persisted = buildOrder(11, payload.getCartDto().getCartId(), payload.getOrderDesc(), payload.getOrderFee());
        when(this.orderRepository.save(any(Order.class))).thenReturn(persisted);

        OrderDto result = this.orderService.save(payload);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(this.orderRepository).save(captor.capture());
        Order savedEntity = captor.getValue();
        assertThat(savedEntity.getOrderId()).isNull();
        assertThat(savedEntity.getOrderDesc()).isEqualTo(payload.getOrderDesc());
        assertThat(savedEntity.getOrderFee()).isEqualTo(payload.getOrderFee());
        assertThat(savedEntity.getCart().getCartId()).isEqualTo(payload.getCartDto().getCartId());

        assertThat(result.getOrderId()).isEqualTo(persisted.getOrderId());
        assertThat(result.getOrderDesc()).isEqualTo(payload.getOrderDesc());
    }

    @Test
    void updateShouldPersistChangesForDtoWithIdentifier() {
        OrderDto payload = buildOrderDto(21, 500, "updated", 333.0);
        Order persisted = buildOrder(payload.getOrderId(), payload.getCartDto().getCartId(), payload.getOrderDesc(), payload.getOrderFee());
        when(this.orderRepository.save(any(Order.class))).thenReturn(persisted);

        OrderDto result = this.orderService.update(payload);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(this.orderRepository).save(captor.capture());
        Order savedEntity = captor.getValue();
        assertThat(savedEntity.getOrderId()).isEqualTo(payload.getOrderId());
        assertThat(savedEntity.getOrderDesc()).isEqualTo(payload.getOrderDesc());
        assertThat(savedEntity.getCart().getCartId()).isEqualTo(payload.getCartDto().getCartId());

        assertThat(result.getOrderId()).isEqualTo(payload.getOrderId());
        assertThat(result.getOrderDesc()).isEqualTo(payload.getOrderDesc());
    }

    @Test
    void updateByIdShouldOverrideIdentifierAndPersistChanges() {
        int orderId = 42;
        OrderDto payload = buildOrderDto(null, 610, "by-id", 441.0);
        Order existing = buildOrder(orderId, 999, "existing", 120.0);
        when(this.orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        Order persisted = buildOrder(orderId, payload.getCartDto().getCartId(), payload.getOrderDesc(), payload.getOrderFee());
        when(this.orderRepository.save(any(Order.class))).thenReturn(persisted);

        OrderDto result = this.orderService.update(orderId, payload);

        verify(this.orderRepository).findById(orderId);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(this.orderRepository).save(captor.capture());
        Order savedEntity = captor.getValue();
        assertThat(savedEntity.getOrderId()).isEqualTo(orderId);
        assertThat(savedEntity.getOrderDesc()).isEqualTo(payload.getOrderDesc());
        assertThat(savedEntity.getCart().getCartId()).isEqualTo(payload.getCartDto().getCartId());
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getOrderDesc()).isEqualTo(payload.getOrderDesc());
    }

    @Test
    void updateByIdShouldThrowWhenOrderMissing() {
        when(this.orderRepository.findById(88)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> this.orderService.update(88, buildOrderDto(null, 710, "missing", 90.0)));
        verify(this.orderRepository).findById(88);
    }

    @Test
    void deleteByIdShouldRemoveExistingOrder() {
        int orderId = 5;
        Order existing = buildOrder(orderId, 320, "to-delete", 76.0);
        when(this.orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        this.orderService.deleteById(orderId);

        verify(this.orderRepository).findById(orderId);
        verify(this.orderRepository).deleteById(orderId);
    }

    @Test
    void deleteByIdShouldThrowWhenOrderMissing() {
        when(this.orderRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> this.orderService.deleteById(99));
        verify(this.orderRepository).findById(99);
        verify(this.orderRepository, times(0)).deleteById(99);
    }

    private static Order buildOrder(Integer orderId, Integer cartId, String desc, double fee) {
        return Order.builder()
                .orderId(orderId)
                .orderDate(NOW)
                .orderDesc(desc)
                .orderFee(fee)
                .cart(Cart.builder().cartId(cartId).build())
                .build();
    }

    private static OrderDto buildOrderDto(Integer orderId, Integer cartId, String desc, double fee) {
        return OrderDto.builder()
                .orderId(orderId)
                .orderDate(NOW)
                .orderDesc(desc)
                .orderFee(fee)
                .cartDto(CartDto.builder().cartId(cartId).build())
                .build();
    }
}
