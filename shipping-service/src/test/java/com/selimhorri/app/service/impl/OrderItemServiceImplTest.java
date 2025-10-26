package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    private OrderItemService orderItemService;

    @BeforeEach
    void setUp() {
        this.orderItemService = new OrderItemServiceImpl(this.orderItemRepository, this.restTemplate);
    }

    @Test
    void findAllShouldReturnMappedDtosWithRemoteDetails() {
        OrderItem entity = buildOrderItem(1, 2, 3);
        when(this.orderItemRepository.findAll()).thenReturn(List.of(entity));
        when(this.restTemplate.getForObject(productUrl(entity.getProductId()), ProductDto.class))
                .thenReturn(ProductDto.builder().productId(entity.getProductId()).productTitle("Item").build());
        when(this.restTemplate.getForObject(orderUrl(entity.getOrderId()), OrderDto.class))
                .thenReturn(OrderDto.builder().orderId(entity.getOrderId()).orderDesc("Order").build());

        List<OrderItemDto> result = this.orderItemService.findAll();

        assertThat(result).hasSize(1);
        OrderItemDto dto = result.get(0);
        assertThat(dto.getProductId()).isEqualTo(entity.getProductId());
        assertThat(dto.getOrderId()).isEqualTo(entity.getOrderId());
        assertThat(dto.getOrderedQuantity()).isEqualTo(entity.getOrderedQuantity());
        assertThat(dto.getProductDto().getProductId()).isEqualTo(entity.getProductId());
        assertThat(dto.getOrderDto().getOrderId()).isEqualTo(entity.getOrderId());

        verify(this.orderItemRepository).findAll();
        verify(this.restTemplate).getForObject(productUrl(entity.getProductId()), ProductDto.class);
        verify(this.restTemplate).getForObject(orderUrl(entity.getOrderId()), OrderDto.class);
    }

    @Test
    void findByIdShouldReturnDtoWhenPresent() {
        OrderItem entity = buildOrderItem(7, 8, 5);
        OrderItemId orderItemId = new OrderItemId(entity.getProductId(), entity.getOrderId());
        when(this.orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(entity));
        when(this.restTemplate.getForObject(productUrl(entity.getProductId()), ProductDto.class))
                .thenReturn(ProductDto.builder().productId(entity.getProductId()).build());
        when(this.restTemplate.getForObject(orderUrl(entity.getOrderId()), OrderDto.class))
                .thenReturn(OrderDto.builder().orderId(entity.getOrderId()).build());

        OrderItemDto result = this.orderItemService.findById(orderItemId);

        assertThat(result.getProductId()).isEqualTo(orderItemId.getProductId());
        assertThat(result.getOrderId()).isEqualTo(orderItemId.getOrderId());
        verify(this.orderItemRepository).findById(orderItemId);
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        OrderItemId orderItemId = new OrderItemId(3, 4);
        when(this.orderItemRepository.findById(orderItemId)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> this.orderItemService.findById(orderItemId));
        verify(this.orderItemRepository).findById(orderItemId);
    }

    @Test
    void saveShouldPersistMappedOrderItem() {
        OrderItemDto payload = buildOrderItemDto(9, 10, 6);
        OrderItem persisted = buildOrderItem(payload.getProductId(), payload.getOrderId(), payload.getOrderedQuantity());
        when(this.orderItemRepository.save(any(OrderItem.class))).thenReturn(persisted);

        OrderItemDto result = this.orderItemService.save(payload);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(this.orderItemRepository).save(captor.capture());
        OrderItem saved = captor.getValue();
        assertThat(saved.getProductId()).isEqualTo(payload.getProductId());
        assertThat(saved.getOrderId()).isEqualTo(payload.getOrderId());
        assertThat(saved.getOrderedQuantity()).isEqualTo(payload.getOrderedQuantity());

        assertThat(result.getProductId()).isEqualTo(payload.getProductId());
        assertThat(result.getOrderId()).isEqualTo(payload.getOrderId());
    }

    @Test
    void updateShouldPersistMappedOrderItem() {
        OrderItemDto payload = buildOrderItemDto(11, 12, 4);
        OrderItem persisted = buildOrderItem(payload.getProductId(), payload.getOrderId(), payload.getOrderedQuantity());
        when(this.orderItemRepository.save(any(OrderItem.class))).thenReturn(persisted);

        OrderItemDto result = this.orderItemService.update(payload);

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(this.orderItemRepository).save(captor.capture());
        OrderItem saved = captor.getValue();
        assertThat(saved.getProductId()).isEqualTo(payload.getProductId());
        assertThat(saved.getOrderId()).isEqualTo(payload.getOrderId());

        assertThat(result.getOrderedQuantity()).isEqualTo(payload.getOrderedQuantity());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        OrderItemId orderItemId = new OrderItemId(13, 14);

        this.orderItemService.deleteById(orderItemId);

        verify(this.orderItemRepository).deleteById(orderItemId);
        verifyNoInteractions(this.restTemplate);
    }

    private static OrderItem buildOrderItem(int productId, int orderId, int quantity) {
        return OrderItem.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(quantity)
                .build();
    }

    private static OrderItemDto buildOrderItemDto(int productId, int orderId, int quantity) {
        return OrderItemDto.builder()
                .productId(productId)
                .orderId(orderId)
                .orderedQuantity(quantity)
                .build();
    }

    private static String productUrl(int productId) {
        return AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
    }

    private static String orderUrl(int orderId) {
        return AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
    }
}
