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
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        this.paymentService = new PaymentServiceImpl(this.paymentRepository, this.restTemplate);
    }

    @Test
    void findAllShouldReturnMappedDtosWithOrderDetails() {
        Payment payment = buildPayment(1, 101, true, PaymentStatus.COMPLETED);
        when(this.paymentRepository.findAll()).thenReturn(List.of(payment));
        OrderDto orderDto = OrderDto.builder().orderId(payment.getOrderId()).build();
        when(this.restTemplate.getForObject(orderUrl(payment.getOrderId()), OrderDto.class)).thenReturn(orderDto);

        List<PaymentDto> result = this.paymentService.findAll();

        assertThat(result).hasSize(1);
        PaymentDto dto = result.get(0);
        assertThat(dto.getPaymentId()).isEqualTo(payment.getPaymentId());
        assertThat(dto.getOrderDto().getOrderId()).isEqualTo(payment.getOrderId());
        assertThat(dto.getPaymentStatus()).isEqualTo(payment.getPaymentStatus());

        verify(this.paymentRepository).findAll();
        verify(this.restTemplate).getForObject(orderUrl(payment.getOrderId()), OrderDto.class);
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        Payment payment = buildPayment(5, 202, false, PaymentStatus.IN_PROGRESS);
        OrderDto orderDto = OrderDto.builder().orderId(payment.getOrderId()).build();
        when(this.paymentRepository.findById(payment.getPaymentId())).thenReturn(Optional.of(payment));
        when(this.restTemplate.getForObject(orderUrl(payment.getOrderId()), OrderDto.class)).thenReturn(orderDto);

        PaymentDto result = this.paymentService.findById(payment.getPaymentId());

        assertThat(result.getPaymentId()).isEqualTo(payment.getPaymentId());
        assertThat(result.getOrderDto().getOrderId()).isEqualTo(payment.getOrderId());
        verify(this.paymentRepository).findById(payment.getPaymentId());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.paymentRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> this.paymentService.findById(99));
        verify(this.paymentRepository).findById(99);
    }

    @Test
    void saveShouldPersistMappedPayment() {
        PaymentDto payload = buildPaymentDto(null, 303, true, PaymentStatus.COMPLETED);
        Payment persisted = buildPayment(10, payload.getOrderDto().getOrderId(), payload.getIsPayed(), payload.getPaymentStatus());
        when(this.paymentRepository.save(any(Payment.class))).thenReturn(persisted);

        PaymentDto result = this.paymentService.save(payload);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(this.paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getPaymentId()).isNull();
        assertThat(saved.getOrderId()).isEqualTo(payload.getOrderDto().getOrderId());
        assertThat(saved.getPaymentStatus()).isEqualTo(payload.getPaymentStatus());

        assertThat(result.getPaymentId()).isEqualTo(persisted.getPaymentId());
        assertThat(result.getOrderDto().getOrderId()).isEqualTo(payload.getOrderDto().getOrderId());
    }

    @Test
    void updateShouldPersistMappedPayment() {
        PaymentDto payload = buildPaymentDto(77, 404, false, PaymentStatus.NOT_STARTED);
        Payment persisted = buildPayment(payload.getPaymentId(), payload.getOrderDto().getOrderId(), payload.getIsPayed(), payload.getPaymentStatus());
        when(this.paymentRepository.save(any(Payment.class))).thenReturn(persisted);

        PaymentDto result = this.paymentService.update(payload);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(this.paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getPaymentId()).isEqualTo(payload.getPaymentId());
        assertThat(saved.getOrderId()).isEqualTo(payload.getOrderDto().getOrderId());
        assertThat(saved.getIsPayed()).isEqualTo(payload.getIsPayed());

        assertThat(result.getPaymentId()).isEqualTo(payload.getPaymentId());
        assertThat(result.getPaymentStatus()).isEqualTo(payload.getPaymentStatus());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.paymentService.deleteById(12);

        verify(this.paymentRepository).deleteById(12);
        verifyNoInteractions(this.restTemplate);
    }

    private static Payment buildPayment(Integer paymentId, Integer orderId, boolean isPayed, PaymentStatus status) {
        return Payment.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .isPayed(isPayed)
                .paymentStatus(status)
                .build();
    }

    private static PaymentDto buildPaymentDto(Integer paymentId, Integer orderId, boolean isPayed, PaymentStatus status) {
        return PaymentDto.builder()
                .paymentId(paymentId)
                .isPayed(isPayed)
                .paymentStatus(status)
                .orderDto(OrderDto.builder().orderId(orderId).build())
                .build();
    }

    private static String orderUrl(Integer orderId) {
        return AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
    }
}
