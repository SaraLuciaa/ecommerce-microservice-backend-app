package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:shipping_service_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
@AutoConfigureMockMvc
class OrderItemResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                .productId(111)
                .orderId(211)
                .orderedQuantity(7)
                .build());
        this.mockServer = MockRestServiceServer.bindTo(this.restTemplate)
                .ignoreExpectOrder(true)
                .build();
    }

    @Test
    void findAllShouldReturnCollectionWithRemoteDetails() throws Exception {
        stubRemoteDetails(this.persistedOrderItem.getProductId(), this.persistedOrderItem.getOrderId());

        this.mockMvc.perform(get("/api/shippings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].productId").value(this.persistedOrderItem.getProductId()))
                .andExpect(jsonPath("$.collection[0].orderId").value(this.persistedOrderItem.getOrderId()))
                .andExpect(jsonPath("$.collection[0].product.productTitle").value("Gaming Mouse"))
                .andExpect(jsonPath("$.collection[0].order.orderDesc").value("Electronics order"));

        this.mockServer.verify();
    }

    @Test
    void findByCompositePathShouldReturnDto() throws Exception {
        stubRemoteDetails(this.persistedOrderItem.getProductId(), this.persistedOrderItem.getOrderId());

        this.mockMvc.perform(get("/api/shippings/{orderId}/{productId}",
                        Integer.toString(this.persistedOrderItem.getOrderId()),
                        Integer.toString(this.persistedOrderItem.getProductId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(this.persistedOrderItem.getProductId()))
                .andExpect(jsonPath("$.orderId").value(this.persistedOrderItem.getOrderId()))
                .andExpect(jsonPath("$.product.productTitle").value("Gaming Mouse"))
                .andExpect(jsonPath("$.order.orderDesc").value("Electronics order"));

        this.mockServer.verify();
    }

    @Test
    void findByBodyShouldReturnDto() throws Exception {
        stubRemoteDetails(this.persistedOrderItem.getProductId(), this.persistedOrderItem.getOrderId());

        this.mockMvc.perform(get("/api/shippings/find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(new OrderItemId(
                                this.persistedOrderItem.getProductId(),
                                this.persistedOrderItem.getOrderId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(this.persistedOrderItem.getProductId()))
                .andExpect(jsonPath("$.orderId").value(this.persistedOrderItem.getOrderId()))
                .andExpect(jsonPath("$.product.productTitle").value("Gaming Mouse"))
                .andExpect(jsonPath("$.order.orderDesc").value("Electronics order"));

        this.mockServer.verify();
    }

    @Test
    void saveShouldPersistOrderItem() throws Exception {
        OrderItemDto payload = OrderItemDto.builder()
                .productId(311)
                .orderId(411)
                .orderedQuantity(5)
                .build();

        this.mockMvc.perform(post("/api/shippings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(311))
                .andExpect(jsonPath("$.orderId").value(411))
                .andExpect(jsonPath("$.orderedQuantity").value(5));

        assertThat(this.orderItemRepository.findById(new OrderItemId(311, 411))).isPresent();
        this.mockServer.verify();
    }

    @Test
    void updateShouldModifyExistingOrderItem() throws Exception {
        OrderItemDto payload = OrderItemDto.builder()
                .productId(this.persistedOrderItem.getProductId())
                .orderId(this.persistedOrderItem.getOrderId())
                .orderedQuantity(12)
                .build();

        this.mockMvc.perform(put("/api/shippings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(12));

        OrderItem reloaded = this.orderItemRepository.findById(new OrderItemId(
                this.persistedOrderItem.getProductId(),
                this.persistedOrderItem.getOrderId())).orElseThrow();
        assertThat(reloaded.getOrderedQuantity()).isEqualTo(12);
        this.mockServer.verify();
    }

    @Test
    void deleteByCompositePathShouldRemoveOrderItem() throws Exception {
        this.mockMvc.perform(delete("/api/shippings/{orderId}/{productId}",
                        Integer.toString(this.persistedOrderItem.getOrderId()),
                        Integer.toString(this.persistedOrderItem.getProductId())))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertThat(this.orderItemRepository.findById(new OrderItemId(
                this.persistedOrderItem.getProductId(),
                this.persistedOrderItem.getOrderId()))).isEmpty();
        this.mockServer.verify();
    }

    @Test
    void deleteByBodyShouldRemoveOrderItem() throws Exception {
        this.mockMvc.perform(delete("/api/shippings/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(new OrderItemId(
                                this.persistedOrderItem.getProductId(),
                                this.persistedOrderItem.getOrderId()))))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

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
                .orderDate(LocalDateTime.of(2023, 2, 10, 9, 45))
                .orderFee(15.75)
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
