package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:payment_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class PaymentResourceIntegrationTest {

    private static final int EXISTING_ORDER_ID = 701;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    private Payment persistedPayment;

    @BeforeEach
    void setUp() {
        this.paymentRepository.deleteAll();

        this.persistedPayment = this.paymentRepository.save(Payment.builder()
                .orderId(EXISTING_ORDER_ID)
                .isPayed(Boolean.FALSE)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build());

        when(this.restTemplate.getForObject(orderUrl(EXISTING_ORDER_ID), OrderDto.class))
                .thenReturn(OrderDto.builder().orderId(EXISTING_ORDER_ID).orderDesc("cart-checkout").build());
    }

    @Test
    void shouldListPayments() throws Exception {
        this.mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].paymentId", equalTo(this.persistedPayment.getPaymentId())))
                .andExpect(jsonPath("$.collection[0].paymentStatus", equalTo(PaymentStatus.NOT_STARTED.name())))
                .andExpect(jsonPath("$.collection[0].order.orderId", equalTo(EXISTING_ORDER_ID)));
    }

    @Test
    void shouldGetPaymentById() throws Exception {
        this.mockMvc.perform(get("/api/payments/{paymentId}", Integer.toString(this.persistedPayment.getPaymentId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", equalTo(this.persistedPayment.getPaymentId())))
                .andExpect(jsonPath("$.paymentStatus", equalTo(PaymentStatus.NOT_STARTED.name())))
                .andExpect(jsonPath("$.order.orderId", equalTo(EXISTING_ORDER_ID)));
    }

    @Test
    void shouldCreatePayment() throws Exception {
        PaymentDto payload = PaymentDto.builder()
                .isPayed(Boolean.TRUE)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder().orderId(702).build())
                .build();

        MvcResult result = this.mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").isNumber())
                .andExpect(jsonPath("$.paymentStatus", equalTo(PaymentStatus.IN_PROGRESS.name())))
                .andExpect(jsonPath("$.order.orderId", equalTo(702)))
                .andReturn();

        PaymentDto created = this.objectMapper.readValue(result.getResponse().getContentAsByteArray(), PaymentDto.class);
        assertThat(this.paymentRepository.findById(created.getPaymentId())).isPresent();
    }

    @Test
    void shouldUpdatePayment() throws Exception {
        PaymentDto payload = PaymentDto.builder()
                .paymentId(this.persistedPayment.getPaymentId())
                .isPayed(Boolean.TRUE)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(OrderDto.builder().orderId(EXISTING_ORDER_ID).build())
                .build();

        this.mockMvc.perform(put("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", equalTo(this.persistedPayment.getPaymentId())))
                .andExpect(jsonPath("$.paymentStatus", equalTo(PaymentStatus.COMPLETED.name())));

        Payment reloaded = this.paymentRepository.findById(this.persistedPayment.getPaymentId()).orElseThrow();
        assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(reloaded.getIsPayed()).isTrue();
    }

    @Test
    void shouldDeletePayment() throws Exception {
        this.mockMvc.perform(delete("/api/payments/{paymentId}", Integer.toString(this.persistedPayment.getPaymentId())))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertThat(this.paymentRepository.findById(this.persistedPayment.getPaymentId())).isEmpty();
    }

    private static String orderUrl(int orderId) {
        return AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
    }
}
