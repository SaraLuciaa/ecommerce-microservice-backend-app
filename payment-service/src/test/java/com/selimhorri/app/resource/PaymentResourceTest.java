package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.service.PaymentService;

@WebMvcTest(PaymentResource.class)
class PaymentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentDto paymentDto;

    @BeforeEach
    void setUp() {
        this.paymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(Boolean.TRUE)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(OrderDto.builder().orderId(55).build())
                .build();
    }

    @Test
    void findAllShouldReturnPaymentsCollection() throws Exception {
        when(this.paymentService.findAll()).thenReturn(List.of(this.paymentDto));

        this.mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].paymentId", equalTo(this.paymentDto.getPaymentId())))
                .andExpect(jsonPath("$.collection[0].isPayed", equalTo(this.paymentDto.getIsPayed())));

        verify(this.paymentService).findAll();
    }

    @Test
    void findByIdShouldReturnPayment() throws Exception {
        when(this.paymentService.findById(1)).thenReturn(this.paymentDto);

        this.mockMvc.perform(get("/api/payments/{paymentId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", equalTo(this.paymentDto.getPaymentId())))
                .andExpect(jsonPath("$.isPayed", equalTo(this.paymentDto.getIsPayed())));

        verify(this.paymentService).findById(1);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        PaymentDto payload = PaymentDto.builder()
                .isPayed(Boolean.FALSE)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder().orderId(99).build())
                .build();
        when(this.paymentService.save(any(PaymentDto.class))).thenReturn(this.paymentDto);

        this.mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", equalTo(this.paymentDto.getPaymentId())));

        ArgumentCaptor<PaymentDto> captor = ArgumentCaptor.forClass(PaymentDto.class);
        verify(this.paymentService).save(captor.capture());
        PaymentDto sent = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(sent.getOrderDto().getOrderId()).isEqualTo(payload.getOrderDto().getOrderId());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.paymentService.update(any(PaymentDto.class))).thenReturn(this.paymentDto);

        this.mockMvc.perform(put("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.paymentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", equalTo(this.paymentDto.getPaymentId())));

        ArgumentCaptor<PaymentDto> captor = ArgumentCaptor.forClass(PaymentDto.class);
        verify(this.paymentService).update(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPaymentId()).isEqualTo(this.paymentDto.getPaymentId());
    }

    @Test
    void deleteByIdShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/payments/{paymentId}", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.paymentService).deleteById(42);
    }
}
