package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:payment_service_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class PaymentServiceIntegrationTest {

    private static final Integer EXISTING_ORDER_ID = 901;

    @Autowired
    private PaymentService paymentService;

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
                .isPayed(Boolean.TRUE)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .build());
    }

    @Test
    void findAllShouldReturnPaymentsWithResolvedOrder() {
        when(this.restTemplate.getForObject(orderUrl(EXISTING_ORDER_ID), OrderDto.class))
                .thenReturn(OrderDto.builder().orderId(EXISTING_ORDER_ID).orderDesc("existing-order").build());

        List<PaymentDto> payments = this.paymentService.findAll();

        assertThat(payments).hasSize(1);
        PaymentDto single = payments.get(0);
        assertThat(single.getPaymentId()).isEqualTo(this.persistedPayment.getPaymentId());
        assertThat(single.getIsPayed()).isTrue();
        assertThat(single.getPaymentStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
        assertThat(single.getOrderDto().getOrderId()).isEqualTo(EXISTING_ORDER_ID);
        assertThat(single.getOrderDto().getOrderDesc()).isEqualTo("existing-order");

        verify(this.restTemplate).getForObject(orderUrl(EXISTING_ORDER_ID), OrderDto.class);
    }

    @Test
    void findByIdShouldReturnPaymentWhenPresent() {
        when(this.restTemplate.getForObject(orderUrl(EXISTING_ORDER_ID), OrderDto.class))
                .thenReturn(OrderDto.builder().orderId(EXISTING_ORDER_ID).orderDesc("detailed-order").build());

        PaymentDto paymentDto = this.paymentService.findById(this.persistedPayment.getPaymentId());

        assertThat(paymentDto.getPaymentId()).isEqualTo(this.persistedPayment.getPaymentId());
        assertThat(paymentDto.getOrderDto().getOrderId()).isEqualTo(EXISTING_ORDER_ID);
        assertThat(paymentDto.getOrderDto().getOrderDesc()).isEqualTo("detailed-order");
    }

    @Test
    void saveShouldPersistPayment() {
        PaymentDto payload = PaymentDto.builder()
                .isPayed(Boolean.FALSE)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(OrderDto.builder().orderId(902).build())
                .build();

        PaymentDto saved = this.paymentService.save(payload);

        assertThat(saved.getPaymentId()).isNotNull();
        Optional<Payment> persisted = this.paymentRepository.findById(saved.getPaymentId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getOrderId()).isEqualTo(902);
        assertThat(persisted.get().getPaymentStatus()).isEqualTo(PaymentStatus.NOT_STARTED);
    }

    @Test
    void updateShouldModifyExistingPayment() {
        PaymentDto payload = PaymentDto.builder()
                .paymentId(this.persistedPayment.getPaymentId())
                .isPayed(Boolean.TRUE)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(OrderDto.builder().orderId(EXISTING_ORDER_ID).build())
                .build();

        PaymentDto updated = this.paymentService.update(payload);

        assertThat(updated.getPaymentId()).isEqualTo(this.persistedPayment.getPaymentId());
        Payment reloaded = this.paymentRepository.findById(this.persistedPayment.getPaymentId()).orElseThrow();
        assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(reloaded.getIsPayed()).isTrue();
    }

    @Test
    void deleteByIdShouldRemovePayment() {
        this.paymentService.deleteById(this.persistedPayment.getPaymentId());

        assertThat(this.paymentRepository.findById(this.persistedPayment.getPaymentId())).isEmpty();
    }

    private static String orderUrl(int orderId) {
        return AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
    }
}
