package com.selimhorri.app.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class PaymentServiceHorizontalIntegrationTest {

	@Autowired
	private PaymentService paymentService;
	
	@Autowired
	private PaymentRepository paymentRepository;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private static WireMockServer wireMockServerOrder;
	
	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public RestTemplate restTemplate() {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(new org.springframework.web.util.DefaultUriBuilderFactory() {
				@Override
				public java.net.URI expand(String uriTemplate, Object... uriVariables) {
					String modifiedUri = uriTemplate
							.replace("http://ORDER-SERVICE", "http://localhost:8091");
					return super.expand(modifiedUri, uriVariables);
				}
				
				@Override
				public java.net.URI expand(String uriTemplate, java.util.Map<String, ?> uriVariables) {
					String modifiedUri = uriTemplate
							.replace("http://ORDER-SERVICE", "http://localhost:8091");
					return super.expand(modifiedUri, uriVariables);
				}
			});
			return restTemplate;
		}
	}
	
	@BeforeAll
	static void startWireMock() {
		wireMockServerOrder = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.port(8091));
		wireMockServerOrder.start();
	}
	
	@AfterAll
	static void stopWireMock() {
		if (wireMockServerOrder != null && wireMockServerOrder.isRunning()) {
			wireMockServerOrder.stop();
		}
	}
	
	@BeforeEach
	void setUp() {
		paymentRepository.deleteAll();
		wireMockServerOrder.resetAll();
	}
	
	@Test
	void whenFindAll_andOrderServiceRespondsSuccessfully_thenReturnPaymentsWithOrderData() 
			throws JsonProcessingException {
		// Given
		Payment payment1 = Payment.builder()
				.orderId(101)
				.isPayed(true)
				.paymentStatus(PaymentStatus.COMPLETED)
				.build();
		Payment payment2 = Payment.builder()
				.orderId(102)
				.isPayed(false)
				.paymentStatus(PaymentStatus.IN_PROGRESS)
				.build();
		paymentRepository.save(payment1);
		paymentRepository.save(payment2);
		
		OrderDto orderDto1 = OrderDto.builder()
				.orderId(101)
				.orderDesc("Order 1 - Electronics")
				.orderFee(1500.50)
				.build();
		
		OrderDto orderDto2 = OrderDto.builder()
				.orderId(102)
				.orderDesc("Order 2 - Books")
				.orderFee(89.99)
				.build();
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/101"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto1))));
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/102"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto2))));
		
		// When
		List<PaymentDto> result = paymentService.findAll();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		
		PaymentDto resultPayment1 = result.stream()
				.filter(p -> p.getPaymentId().equals(payment1.getPaymentId()))
				.findFirst()
				.orElse(null);
		assertThat(resultPayment1).isNotNull();
		assertThat(resultPayment1.getIsPayed()).isTrue();
		assertThat(resultPayment1.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(resultPayment1.getOrderDto()).isNotNull();
		assertThat(resultPayment1.getOrderDto().getOrderDesc()).isEqualTo("Order 1 - Electronics");
		assertThat(resultPayment1.getOrderDto().getOrderFee()).isEqualTo(1500.50);
		
		PaymentDto resultPayment2 = result.stream()
				.filter(p -> p.getPaymentId().equals(payment2.getPaymentId()))
				.findFirst()
				.orElse(null);
		assertThat(resultPayment2).isNotNull();
		assertThat(resultPayment2.getIsPayed()).isFalse();
		assertThat(resultPayment2.getPaymentStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
		assertThat(resultPayment2.getOrderDto()).isNotNull();
		assertThat(resultPayment2.getOrderDto().getOrderDesc()).isEqualTo("Order 2 - Books");
		assertThat(resultPayment2.getOrderDto().getOrderFee()).isEqualTo(89.99);
		
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/101")));
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/102")));
	}
	
	@Test
	void whenFindAll_andOrderServiceDelayResponse_thenEventuallyReturnsAllPayments() 
			throws JsonProcessingException {
		// Given
		Payment payment1 = Payment.builder()
				.orderId(201)
				.isPayed(true)
				.paymentStatus(PaymentStatus.COMPLETED)
				.build();
		
		paymentRepository.save(payment1);
		
		OrderDto orderDto1 = OrderDto.builder()
				.orderId(201)
				.orderDesc("Delayed Order")
				.orderFee(999.99)
				.build();
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/201"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto1))
						.withFixedDelay(400)));
		
		// When
		long startTime = System.currentTimeMillis();
		List<PaymentDto> result = paymentService.findAll();
		long endTime = System.currentTimeMillis();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOrderDto()).isNotNull();
		assertThat(result.get(0).getOrderDto().getOrderDesc()).isEqualTo("Delayed Order");
		assertThat(endTime - startTime).isGreaterThanOrEqualTo(400);
		
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/201")));
	}
	
	@Test
	void whenFindById_andOrderServiceRespondsSuccessfully_thenReturnPaymentWithOrderData() 
			throws JsonProcessingException {
		// Given
		Payment payment = Payment.builder()
				.orderId(301)
				.isPayed(true)
				.paymentStatus(PaymentStatus.COMPLETED)
				.build();
		Payment savedPayment = paymentRepository.save(payment);
		
		OrderDto orderDto = OrderDto.builder()
				.orderId(301)
				.orderDesc("Single Order")
				.orderFee(2500.00)
				.build();
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/301"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto))));
		
		// When
		PaymentDto result = paymentService.findById(savedPayment.getPaymentId());
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getPaymentId()).isEqualTo(savedPayment.getPaymentId());
		assertThat(result.getIsPayed()).isTrue();
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(result.getOrderDto()).isNotNull();
		assertThat(result.getOrderDto().getOrderId()).isEqualTo(301);
		assertThat(result.getOrderDto().getOrderDesc()).isEqualTo("Single Order");
		assertThat(result.getOrderDto().getOrderFee()).isEqualTo(2500.00);
		
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/301")));
	}
	
	@Test
	void whenFindById_andOrderServiceDelayResponse_thenEventuallyReturnsPayment() 
			throws JsonProcessingException {
		// Given
		Payment payment = Payment.builder()
				.orderId(401)
				.isPayed(false)
				.paymentStatus(PaymentStatus.IN_PROGRESS)
				.build();
		Payment savedPayment = paymentRepository.save(payment);
		
		OrderDto orderDto = OrderDto.builder()
				.orderId(401)
				.orderDesc("Order 401")
                .orderFee(150.00)
				.build();
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/401"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto))
						.withFixedDelay(350)));
		
		// When
		long startTime = System.currentTimeMillis();
		PaymentDto result = paymentService.findById(savedPayment.getPaymentId());
		long endTime = System.currentTimeMillis();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getPaymentId()).isEqualTo(savedPayment.getPaymentId());
		assertThat(result.getOrderDto()).isNotNull();
		assertThat(result.getOrderDto().getOrderDesc()).isEqualTo("Order 401");
		assertThat(endTime - startTime).isGreaterThanOrEqualTo(350);
		
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/401")));
	}
}
