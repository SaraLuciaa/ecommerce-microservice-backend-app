package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:shipping_service_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class OrderItemServiceIntegrationTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer mockServer;
    private OrderItem persistedOrderItem;

    @BeforeEach
    void setUp() {
        this.orderItemRepository.deleteAll();
        this.persistedOrderItem = this.orderItemRepository.save(OrderItem.builder()
                .productId(101)
                .orderId(201)
                .orderedQuantity(4)
                .build());
        this.mockServer = MockRestServiceServer.bindTo(this.restTemplate)
                .ignoreExpectOrder(true)
                .build();
    }

    @Test
    void findAllShouldReturnOrderItemsWithRemoteDetails() throws Exception {
        stubRemoteDetails(this.persistedOrderItem.getProductId(), this.persistedOrderItem.getOrderId());

        List<OrderItemDto> result = this.orderItemService.findAll();

        assertThat(result).hasSize(1);
        OrderItemDto dto = result.get(0);
        assertThat(dto.getProductId()).isEqualTo(this.persistedOrderItem.getProductId());
        assertThat(dto.getOrderId()).isEqualTo(this.persistedOrderItem.getOrderId());
        assertThat(dto.getOrderedQuantity()).isEqualTo(this.persistedOrderItem.getOrderedQuantity());
        assertThat(dto.getProductDto().getProductTitle()).isEqualTo("Gaming Mouse");
        assertThat(dto.getOrderDto().getOrderDesc()).isEqualTo("Electronics order");

        this.mockServer.verify();
    }

    @Test
    void findByIdShouldReturnOrderItemWhenPresent() throws Exception {
        stubRemoteDetails(this.persistedOrderItem.getProductId(), this.persistedOrderItem.getOrderId());

        OrderItemDto dto = this.orderItemService.findById(new OrderItemId(
                this.persistedOrderItem.getProductId(),
                this.persistedOrderItem.getOrderId()));

        assertThat(dto.getOrderedQuantity()).isEqualTo(this.persistedOrderItem.getOrderedQuantity());
        assertThat(dto.getProductDto().getProductId()).isEqualTo(this.persistedOrderItem.getProductId());
        assertThat(dto.getOrderDto().getOrderId()).isEqualTo(this.persistedOrderItem.getOrderId());

        this.mockServer.verify();
    }

    @Test
    void saveShouldPersistOrderItem() {
        OrderItemDto payload = OrderItemDto.builder()
                .productId(301)
                .orderId(401)
                .orderedQuantity(9)
                .build();

        OrderItemDto saved = this.orderItemService.save(payload);

        assertThat(saved.getProductId()).isEqualTo(301);
        assertThat(saved.getOrderId()).isEqualTo(401);
        OrderItem reloaded = this.orderItemRepository.findById(new OrderItemId(301, 401)).orElseThrow();
        assertThat(reloaded.getOrderedQuantity()).isEqualTo(9);

        this.mockServer.verify();
    }

    @Test
    void updateShouldModifyExistingOrderItem() {
        OrderItemDto payload = OrderItemDto.builder()
                .productId(this.persistedOrderItem.getProductId())
                .orderId(this.persistedOrderItem.getOrderId())
                .orderedQuantity(11)
                .build();

        OrderItemDto updated = this.orderItemService.update(payload);

        assertThat(updated.getOrderedQuantity()).isEqualTo(11);
        OrderItem reloaded = this.orderItemRepository.findById(new OrderItemId(
                this.persistedOrderItem.getProductId(),
                this.persistedOrderItem.getOrderId())).orElseThrow();
        assertThat(reloaded.getOrderedQuantity()).isEqualTo(11);

        this.mockServer.verify();
    }

    @Test
    void deleteByIdShouldRemoveOrderItem() {
        this.orderItemService.deleteById(new OrderItemId(
                this.persistedOrderItem.getProductId(),
                this.persistedOrderItem.getOrderId()));

        assertThat(this.orderItemRepository.findById(new OrderItemId(
                this.persistedOrderItem.getProductId(),
                this.persistedOrderItem.getOrderId()))).isEmpty();

        this.mockServer.verify();
    }

    private void stubRemoteDetails(int productId, int orderId) throws Exception {
        ProductDto productDto = ProductDto.builder()
                .productId(productId)
                .productTitle("Gaming Mouse")
                .imageUrl("mouse.png")
                .build();
        OrderDto orderDto = OrderDto.builder()
                .orderId(orderId)
                .orderDesc("Electronics order")
                .orderDate(LocalDateTime.of(2023, 1, 5, 10, 15))
                .orderFee(19.99)
                .build();

        this.mockServer.expect(ExpectedCount.once(), requestTo(
                        AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId))
                .andRespond(withSuccess(this.objectMapper.writeValueAsString(productDto), MediaType.APPLICATION_JSON));
        this.mockServer.expect(ExpectedCount.once(), requestTo(
                        AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId))
                .andRespond(withSuccess(this.objectMapper.writeValueAsString(orderDto), MediaType.APPLICATION_JSON));
    }

        @TestConfiguration
        static class RestTemplateTestConfig {

                @Bean
                @Primary
                RestTemplate testRestTemplate() {
                        return new RestTemplate();
                }
        }
}
