package com.selimhorri.app.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.yml")
class OrderServiceHorizontalIntegrationTest {

	@Autowired
	private CartService cartService;
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private static WireMockServer wireMockServer;
	
	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public RestTemplate restTemplate() {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(new org.springframework.web.util.DefaultUriBuilderFactory() {
				@Override
				public java.net.URI expand(String uriTemplate, Object... uriVariables) {
					String modifiedUri = uriTemplate.replace("http://USER-SERVICE", "http://localhost:8089");
					return super.expand(modifiedUri, uriVariables);
				}
				
				@Override
				public java.net.URI expand(String uriTemplate, java.util.Map<String, ?> uriVariables) {
					String modifiedUri = uriTemplate.replace("http://USER-SERVICE", "http://localhost:8089");
					return super.expand(modifiedUri, uriVariables);
				}
			});
			return restTemplate;
		}
	}
	
	@BeforeAll
	static void startWireMock() {
		wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.port(8089));
		wireMockServer.start();
		configureFor("localhost", 8089);
	}
	
	@AfterAll
	static void stopWireMock() {
		if (wireMockServer != null && wireMockServer.isRunning()) {
			wireMockServer.stop();
		}
	}
	
	@BeforeEach
	void setUp() {
		cartRepository.deleteAll();
		wireMockServer.resetAll();
	}
	
	@Test
	void whenFindById_andUserServiceRespondsSuccessfully_thenReturnCartWithUserData() throws JsonProcessingException {
		// Given
		Cart cart = Cart.builder()
				.userId(1)
				.build();
		Cart savedCart = cartRepository.save(cart);
		
		UserDto userDto = UserDto.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.email("john.doe@example.com")
				.phone("1234567890")
				.imageUrl("http://example.com/image.jpg")
				.build();
				
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto))));
		
		// When
		CartDto result = cartService.findById(savedCart.getCartId());
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getCartId()).isEqualTo(savedCart.getCartId());
		assertThat(result.getUserId()).isEqualTo(1);
		assertThat(result.getUserDto()).isNotNull();
		assertThat(result.getUserDto().getUserId()).isEqualTo(1);
		assertThat(result.getUserDto().getFirstName()).isEqualTo("John");
		assertThat(result.getUserDto().getLastName()).isEqualTo("Doe");
		assertThat(result.getUserDto().getEmail()).isEqualTo("john.doe@example.com");
		
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart.getUserId())));
	}
	
	@Test
	void whenFindAll_andUserServiceRespondsSuccessfully_thenReturnCartsWithUserData() throws JsonProcessingException {
		// Given
		Cart cart1 = Cart.builder().userId(1).build();
		Cart cart2 = Cart.builder().userId(2).build();
		Cart savedCart1 = cartRepository.save(cart1);
		Cart savedCart2 = cartRepository.save(cart2);
		
		UserDto userDto1 = UserDto.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.email("john.doe@example.com")
				.phone("1234567890")
				.build();
		
		UserDto userDto2 = UserDto.builder()
				.userId(2)
				.firstName("Jane")
				.lastName("Smith")
				.email("jane.smith@example.com")
				.phone("0987654321")
				.build();
		
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart1.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto1))));
		
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart2.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto2))));
		
		// When
		List<CartDto> result = cartService.findAll();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		
		CartDto resultCart1 = result.stream()
				.filter(c -> c.getCartId().equals(savedCart1.getCartId()))
				.findFirst()
				.orElse(null);
		assertThat(resultCart1).isNotNull();
		assertThat(resultCart1.getUserDto()).isNotNull();
		assertThat(resultCart1.getUserDto().getFirstName()).isEqualTo("John");
		
		CartDto resultCart2 = result.stream()
				.filter(c -> c.getCartId().equals(savedCart2.getCartId()))
				.findFirst()
				.orElse(null);
		assertThat(resultCart2).isNotNull();
		assertThat(resultCart2.getUserDto()).isNotNull();
		assertThat(resultCart2.getUserDto().getFirstName()).isEqualTo("Jane");
		
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart1.getUserId())));
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart2.getUserId())));
	}
	
	@Test
	void whenFindById_andUserServiceReturns404_thenThrowException() {
		// Given
		Cart cart = Cart.builder()
				.userId(999)
				.build();
		Cart savedCart = cartRepository.save(cart);
		
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.NOT_FOUND.value())));
		
		// When & Then
		assertThrows(Exception.class, () -> cartService.findById(savedCart.getCartId()));
		
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart.getUserId())));
	}
	
	@Test
	void whenFindById_andUserServiceIsDown_thenThrowException() {
		// Given
		Cart cart = Cart.builder()
				.userId(1)
				.build();
		Cart savedCart = cartRepository.save(cart);
		
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())));
		
		// When & Then
		assertThrows(Exception.class, () -> cartService.findById(savedCart.getCartId()));
		
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart.getUserId())));
	}
	
	@Test
	void whenFindById_andUserServiceReturnsInvalidJson_thenThrowException() {
		// Given
		Cart cart = Cart.builder()
				.userId(1)
				.build();
		Cart savedCart = cartRepository.save(cart);
		
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("{invalid json}")));
		
		// When & Then
		assertThrows(Exception.class, () -> cartService.findById(savedCart.getCartId()));
		
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart.getUserId())));
	}
	
	@Test
	void whenFindById_andUserServiceDelaysResponse_thenEventuallyReturnsData() throws JsonProcessingException {
		// Given
		Cart cart = Cart.builder()
				.userId(1)
				.build();
		Cart savedCart = cartRepository.save(cart);
		
		UserDto userDto = UserDto.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.email("john.doe@example.com")
				.build();
		
		wireMockServer.stubFor(get(urlEqualTo("/user-service/api/users/" + savedCart.getUserId()))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto))
						.withFixedDelay(1000)));
		
		// When
		long startTime = System.currentTimeMillis();
		CartDto result = cartService.findById(savedCart.getCartId());
		long endTime = System.currentTimeMillis();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result.getUserDto()).isNotNull();
		assertThat(result.getUserDto().getFirstName()).isEqualTo("John");
		assertThat(endTime - startTime).isGreaterThanOrEqualTo(1000);
		
		wireMockServer.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/" + savedCart.getUserId())));
	}
}