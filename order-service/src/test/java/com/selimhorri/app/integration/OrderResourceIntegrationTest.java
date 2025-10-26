package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:order_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class OrderResourceIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);
    private static final LocalDateTime EXISTING_ORDER_DATE = LocalDateTime.of(2024, 5, 1, 14, 0, 0, 321_000_000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .userId(777)
                .build());

        this.persistedOrder = this.orderRepository.save(Order.builder()
                .orderDate(EXISTING_ORDER_DATE)
                .orderDesc("existing-order")
                .orderFee(75.30)
                .cart(this.persistedCart)
                .build());
    }

    @Test
    void shouldListOrders() throws Exception {
        this.mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].orderId", equalTo(this.persistedOrder.getOrderId())))
                .andExpect(jsonPath("$.collection[0].orderDesc", equalTo("existing-order")))
                .andExpect(jsonPath("$.collection[0].cart.cartId", equalTo(this.persistedCart.getCartId())))
                .andExpect(jsonPath("$.collection[0].orderDate", equalTo(EXISTING_ORDER_DATE.format(FORMATTER))));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        this.mockMvc.perform(get("/api/orders/{orderId}", Integer.toString(this.persistedOrder.getOrderId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", equalTo(this.persistedOrder.getOrderId())))
                .andExpect(jsonPath("$.orderDesc", equalTo("existing-order")))
                .andExpect(jsonPath("$.cart.cartId", equalTo(this.persistedCart.getCartId())));
    }

    @Test
    void shouldCreateOrder() throws Exception {
        Cart anotherCart = this.cartRepository.save(Cart.builder()
                .userId(888)
                .build());

        OrderDto payload = OrderDto.builder()
                .orderDate(EXISTING_ORDER_DATE.plusDays(1))
                .orderDesc("new-order")
                .orderFee(110.99)
                .cartDto(CartDto.builder().cartId(anotherCart.getCartId()).build())
                .build();

        MvcResult result = this.mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.orderDesc", equalTo("new-order")))
                .andExpect(jsonPath("$.cart.cartId", equalTo(anotherCart.getCartId())))
                .andReturn();

        OrderDto saved = this.objectMapper.readValue(result.getResponse().getContentAsByteArray(), OrderDto.class);
        assertThat(this.orderRepository.findById(saved.getOrderId())).isPresent();
    }

    @Test
    void shouldUpdateOrder() throws Exception {
        OrderDto payload = OrderDto.builder()
                .orderId(this.persistedOrder.getOrderId())
                .orderDate(this.persistedOrder.getOrderDate().plusDays(2))
                .orderDesc("updated-order")
                .orderFee(200.45)
                .cartDto(CartDto.builder().cartId(this.persistedCart.getCartId()).build())
                .build();

        this.mockMvc.perform(put("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", equalTo(this.persistedOrder.getOrderId())))
                .andExpect(jsonPath("$.orderDesc", equalTo("updated-order")));

        Order reloaded = this.orderRepository.findById(this.persistedOrder.getOrderId()).orElseThrow();
        assertThat(reloaded.getOrderDesc()).isEqualTo("updated-order");
        assertThat(reloaded.getOrderFee()).isEqualTo(200.45);
    }

    @Test
    void shouldPatchOrderById() throws Exception {
        OrderDto payload = OrderDto.builder()
                .orderDate(EXISTING_ORDER_DATE.plusDays(5))
                .orderDesc("patched-order")
                .orderFee(310.00)
                .cartDto(CartDto.builder().cartId(this.persistedCart.getCartId()).build())
                .build();

        this.mockMvc.perform(put("/api/orders/{orderId}", Integer.toString(this.persistedOrder.getOrderId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", equalTo(this.persistedOrder.getOrderId())))
                .andExpect(jsonPath("$.orderDesc", equalTo("patched-order")));

        Order patched = this.orderRepository.findById(this.persistedOrder.getOrderId()).orElseThrow();
        assertThat(patched.getOrderDesc()).isEqualTo("patched-order");
        assertThat(patched.getOrderFee()).isEqualTo(310.00);
    }

    @Test
    void shouldDeleteOrder() throws Exception {
        this.mockMvc.perform(delete("/api/orders/{orderId}", Integer.toString(this.persistedOrder.getOrderId())))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertThat(this.orderRepository.findById(this.persistedOrder.getOrderId())).isEmpty();
    }
}
