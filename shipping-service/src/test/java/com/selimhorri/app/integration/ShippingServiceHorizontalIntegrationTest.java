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
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ShippingServiceHorizontalIntegrationTest {

	@Autowired
	private OrderItemService orderItemService;
	
	@Autowired
	private OrderItemRepository orderItemRepository;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private static WireMockServer wireMockServerProduct;
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
							.replace("http://PRODUCT-SERVICE", "http://localhost:8092")
							.replace("http://ORDER-SERVICE", "http://localhost:8093");
					return super.expand(modifiedUri, uriVariables);
				}
				
				@Override
				public java.net.URI expand(String uriTemplate, java.util.Map<String, ?> uriVariables) {
					String modifiedUri = uriTemplate
							.replace("http://PRODUCT-SERVICE", "http://localhost:8092")
							.replace("http://ORDER-SERVICE", "http://localhost:8093");
					return super.expand(modifiedUri, uriVariables);
				}
			});
			return restTemplate;
		}
	}
	
	@BeforeAll
	static void startWireMock() {
		wireMockServerProduct = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.port(8092));
		wireMockServerOrder = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.port(8093));
		wireMockServerProduct.start();
		wireMockServerOrder.start();
	}
	
	@AfterAll
	static void stopWireMock() {
		if (wireMockServerProduct != null && wireMockServerProduct.isRunning()) {
			wireMockServerProduct.stop();
		}
		if (wireMockServerOrder != null && wireMockServerOrder.isRunning()) {
			wireMockServerOrder.stop();
		}
	}
	
	@BeforeEach
	void setUp() {
		orderItemRepository.deleteAll();
		wireMockServerProduct.resetAll();
		wireMockServerOrder.resetAll();
	}
	
	@Test
	void whenFindAll_andProductAndOrderServicesRespondSuccessfully_thenReturnOrderItemsWithEnrichedData() 
			throws JsonProcessingException {
		// Given
		OrderItem orderItem1 = OrderItem.builder()
				.orderId(501)
				.productId(101)
				.orderedQuantity(5)
				.build();
		OrderItem orderItem2 = OrderItem.builder()
				.orderId(502)
				.productId(102)
				.orderedQuantity(3)
				.build();
		orderItemRepository.save(orderItem1);
		orderItemRepository.save(orderItem2);
		
		ProductDto productDto1 = ProductDto.builder()
				.productId(101)
				.productTitle("Laptop Pro")
				.imageUrl("http://example.com/laptop.jpg")
				.sku("LAP-001")
				.priceUnit(1299.99)
				.quantity(10)
				.build();
		
		ProductDto productDto2 = ProductDto.builder()
				.productId(102)
				.productTitle("Wireless Mouse")
				.imageUrl("http://example.com/mouse.jpg")
				.sku("MOUSE-001")
				.priceUnit(29.99)
				.quantity(100)
				.build();
		
		OrderDto orderDto1 = OrderDto.builder()
				.orderId(501)
				.orderDesc("Order for Electronics")
				.orderFee(1500.00)
				.build();
		
		OrderDto orderDto2 = OrderDto.builder()
				.orderId(502)
				.orderDesc("Order for Accessories")
				.orderFee(150.00)
				.build();
		
		// Stub Product Service
		wireMockServerProduct.stubFor(get(urlEqualTo("/product-service/api/products/101"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(productDto1))));
		
		wireMockServerProduct.stubFor(get(urlEqualTo("/product-service/api/products/102"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(productDto2))));
		
		// Stub Order Service
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/501"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto1))));
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/502"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto2))));
		
		// When
		List<OrderItemDto> result = orderItemService.findAll();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		
		OrderItemDto resultItem1 = result.stream()
				.filter(oi -> oi.getOrderId().equals(501))
				.findFirst()
				.orElse(null);
		assertThat(resultItem1).isNotNull();
		assertThat(resultItem1.getOrderedQuantity()).isEqualTo(5);
		assertThat(resultItem1.getProductDto()).isNotNull();
		assertThat(resultItem1.getProductDto().getProductTitle()).isEqualTo("Laptop Pro");
		assertThat(resultItem1.getProductDto().getPriceUnit()).isEqualTo(1299.99);
		assertThat(resultItem1.getOrderDto()).isNotNull();
		assertThat(resultItem1.getOrderDto().getOrderDesc()).isEqualTo("Order for Electronics");
		assertThat(resultItem1.getOrderDto().getOrderFee()).isEqualTo(1500.00);
		
		OrderItemDto resultItem2 = result.stream()
				.filter(oi -> oi.getOrderId().equals(502))
				.findFirst()
				.orElse(null);
		assertThat(resultItem2).isNotNull();
		assertThat(resultItem2.getOrderedQuantity()).isEqualTo(3);
		assertThat(resultItem2.getProductDto()).isNotNull();
		assertThat(resultItem2.getProductDto().getProductTitle()).isEqualTo("Wireless Mouse");
		assertThat(resultItem2.getProductDto().getPriceUnit()).isEqualTo(29.99);
		assertThat(resultItem2.getOrderDto()).isNotNull();
		assertThat(resultItem2.getOrderDto().getOrderDesc()).isEqualTo("Order for Accessories");
		assertThat(resultItem2.getOrderDto().getOrderFee()).isEqualTo(150.00);
		
		// Verify HTTP calls
		wireMockServerProduct.verify(2, getRequestedFor(urlMatching("/product-service/api/products/.*")));
		wireMockServerOrder.verify(2, getRequestedFor(urlMatching("/order-service/api/orders/.*")));
	}
	
	@Test
	void whenFindAll_andBothServicesDelayResponse_thenEventuallyReturnsAllOrderItems() 
			throws JsonProcessingException {
		// Given
		OrderItem orderItem = OrderItem.builder()
				.orderId(601)
				.productId(201)
				.orderedQuantity(2)
				.build();
		orderItemRepository.save(orderItem);
		
		ProductDto productDto = ProductDto.builder()
				.productId(201)
				.productTitle("Keyboard Mechanical")
				.imageUrl("http://example.com/keyboard.jpg")
				.sku("KEYBOARD-001")
				.priceUnit(99.99)
				.quantity(50)
				.build();
		
		OrderDto orderDto = OrderDto.builder()
				.orderId(601)
				.orderDesc("Delayed Order")
				.orderFee(250.00)
				.build();
		
		// Stub with delays
		wireMockServerProduct.stubFor(get(urlEqualTo("/product-service/api/products/201"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(productDto))
						.withFixedDelay(300)));
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/601"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto))
						.withFixedDelay(300)));
		
		// When
		long startTime = System.currentTimeMillis();
		List<OrderItemDto> result = orderItemService.findAll();
		long endTime = System.currentTimeMillis();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getProductDto()).isNotNull();
		assertThat(result.get(0).getOrderDto()).isNotNull();
		assertThat(endTime - startTime).isGreaterThanOrEqualTo(300);
		
		wireMockServerProduct.verify(1, getRequestedFor(urlEqualTo("/product-service/api/products/201")));
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/601")));
	}
	
	@Test
	void whenFindById_andProductAndOrderServicesRespondSuccessfully_thenReturnOrderItemWithEnrichedData() 
			throws JsonProcessingException {
		// Given
		OrderItem orderItem = OrderItem.builder()
				.orderId(701)
				.productId(301)
				.orderedQuantity(1)
				.build();
		orderItemRepository.save(orderItem);
		
		ProductDto productDto = ProductDto.builder()
				.productId(301)
				.productTitle("USB-C Cable")
				.imageUrl("http://example.com/cable.jpg")
				.sku("CABLE-001")
				.priceUnit(15.99)
				.quantity(200)
				.build();
		
		OrderDto orderDto = OrderDto.builder()
				.orderId(701)
				.orderDesc("Single Order Item")
				.orderFee(75.00)
				.build();
		
		wireMockServerProduct.stubFor(get(urlEqualTo("/product-service/api/products/301"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(productDto))));
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/701"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto))));
		
		// When
		OrderItemId itemId = new OrderItemId(301, 701);
		OrderItemDto result = orderItemService.findById(itemId);
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getOrderId()).isEqualTo(701);
		assertThat(result.getProductId()).isEqualTo(301);
		assertThat(result.getOrderedQuantity()).isEqualTo(1);
		assertThat(result.getProductDto()).isNotNull();
		assertThat(result.getProductDto().getProductTitle()).isEqualTo("USB-C Cable");
		assertThat(result.getProductDto().getPriceUnit()).isEqualTo(15.99);
		assertThat(result.getOrderDto()).isNotNull();
		assertThat(result.getOrderDto().getOrderDesc()).isEqualTo("Single Order Item");
		assertThat(result.getOrderDto().getOrderFee()).isEqualTo(75.00);
		
		wireMockServerProduct.verify(1, getRequestedFor(urlEqualTo("/product-service/api/products/301")));
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/701")));
	}
	
	@Test
	void whenFindById_andBothServicesDelayResponse_thenEventuallyReturnsOrderItem() 
			throws JsonProcessingException {
		// Given
		OrderItem orderItem = OrderItem.builder()
				.orderId(801)
				.productId(401)
				.orderedQuantity(4)
				.build();
		orderItemRepository.save(orderItem);
		
		ProductDto productDto = ProductDto.builder()
				.productId(401)
				.productTitle("Monitor 4K")
				.imageUrl("http://example.com/monitor.jpg")
				.sku("MONITOR-001")
				.priceUnit(399.99)
				.quantity(20)
				.build();
		
		OrderDto orderDto = OrderDto.builder()
				.orderId(801)
				.orderDesc("Delayed Single Item")
				.orderFee(500.00)
				.build();
		
		wireMockServerProduct.stubFor(get(urlEqualTo("/product-service/api/products/401"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(productDto))
						.withFixedDelay(250)));
		
		wireMockServerOrder.stubFor(get(urlEqualTo("/order-service/api/orders/801"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(orderDto))
						.withFixedDelay(250)));
		
		// When
		long startTime = System.currentTimeMillis();
		OrderItemId itemId = new OrderItemId(401, 801);
		OrderItemDto result = orderItemService.findById(itemId);
		long endTime = System.currentTimeMillis();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getOrderId()).isEqualTo(801);
		assertThat(result.getProductId()).isEqualTo(401);
		assertThat(result.getOrderedQuantity()).isEqualTo(4);
		assertThat(result.getProductDto()).isNotNull();
		assertThat(result.getProductDto().getProductTitle()).isEqualTo("Monitor 4K");
		assertThat(result.getOrderDto()).isNotNull();
		assertThat(result.getOrderDto().getOrderDesc()).isEqualTo("Delayed Single Item");
		assertThat(endTime - startTime).isGreaterThanOrEqualTo(250);
		
		wireMockServerProduct.verify(1, getRequestedFor(urlEqualTo("/product-service/api/products/401")));
		wireMockServerOrder.verify(1, getRequestedFor(urlEqualTo("/order-service/api/orders/801")));
	}
}
