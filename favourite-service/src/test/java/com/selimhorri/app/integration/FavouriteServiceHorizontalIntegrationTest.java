package com.selimhorri.app.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.yml")
class FavouriteServiceHorizontalIntegrationTest {

	@Autowired
	private FavouriteService favouriteService;
	
	@Autowired
	private FavouriteRepository favouriteRepository;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private static WireMockServer wireMockServerUser;
	private static WireMockServer wireMockServerProduct;
	
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
							.replace("http://USER-SERVICE", "http://localhost:8089")
							.replace("http://PRODUCT-SERVICE", "http://localhost:8090");
					return super.expand(modifiedUri, uriVariables);
				}
				
				@Override
				public java.net.URI expand(String uriTemplate, java.util.Map<String, ?> uriVariables) {
					String modifiedUri = uriTemplate
							.replace("http://USER-SERVICE", "http://localhost:8089")
							.replace("http://PRODUCT-SERVICE", "http://localhost:8090");
					return super.expand(modifiedUri, uriVariables);
				}
			});
			return restTemplate;
		}
	}
	
	@BeforeAll
	static void startWireMock() {
		wireMockServerUser = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.port(8089));
		wireMockServerUser.start();
		
		wireMockServerProduct = new WireMockServer(WireMockConfiguration.wireMockConfig()
				.port(8090));
		wireMockServerProduct.start();
	}
	
	@AfterAll
	static void stopWireMock() {
		if (wireMockServerUser != null && wireMockServerUser.isRunning()) {
			wireMockServerUser.stop();
		}
		if (wireMockServerProduct != null && wireMockServerProduct.isRunning()) {
			wireMockServerProduct.stop();
		}
	}
	
	@BeforeEach
	void setUp() {
		favouriteRepository.deleteAll();
		wireMockServerUser.resetAll();
		wireMockServerProduct.resetAll();
	}
	
	@Test
	void whenFindAll_andUserAndProductServicesRespondSuccessfully_thenReturnFavouritesWithUserAndProductData() 
			throws JsonProcessingException {
		// Given
		Favourite favourite1 = Favourite.builder()
				.userId(1)
				.productId(101)
				.likeDate(LocalDateTime.now())
				.build();
		Favourite favourite2 = Favourite.builder()
				.userId(2)
				.productId(102)
				.likeDate(LocalDateTime.now())
				.build();
		favouriteRepository.save(favourite1);
		favouriteRepository.save(favourite2);
		
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
		
		ProductDto productDto1 = ProductDto.builder()
				.productId(101)
				.productTitle("Laptop")
				.sku("SKU-101")
				.priceUnit(999.99)
				.quantity(10)
				.build();
		
		ProductDto productDto2 = ProductDto.builder()
				.productId(102)
				.productTitle("Mouse")
				.sku("SKU-102")
				.priceUnit(29.99)
				.quantity(50)
				.build();
		
		wireMockServerUser.stubFor(get(urlEqualTo("/user-service/api/users/1"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto1))));
		
		wireMockServerUser.stubFor(get(urlEqualTo("/user-service/api/users/2"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto2))));
		
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
		
		// When
		List<FavouriteDto> result = favouriteService.findAll();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		
		FavouriteDto resultFav1 = result.stream()
				.filter(f -> f.getUserId().equals(1) && f.getProductId().equals(101))
				.findFirst()
				.orElse(null);
		assertThat(resultFav1).isNotNull();
		assertThat(resultFav1.getUserDto()).isNotNull();
		assertThat(resultFav1.getUserDto().getFirstName()).isEqualTo("John");
		assertThat(resultFav1.getProductDto()).isNotNull();
		assertThat(resultFav1.getProductDto().getProductTitle()).isEqualTo("Laptop");
		
		FavouriteDto resultFav2 = result.stream()
				.filter(f -> f.getUserId().equals(2) && f.getProductId().equals(102))
				.findFirst()
				.orElse(null);
		assertThat(resultFav2).isNotNull();
		assertThat(resultFav2.getUserDto()).isNotNull();
		assertThat(resultFav2.getUserDto().getFirstName()).isEqualTo("Jane");
		assertThat(resultFav2.getProductDto()).isNotNull();
		assertThat(resultFav2.getProductDto().getProductTitle()).isEqualTo("Mouse");
		
		wireMockServerUser.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/1")));
		wireMockServerUser.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/2")));
		
		wireMockServerProduct.verify(1, getRequestedFor(urlEqualTo("/product-service/api/products/101")));
		wireMockServerProduct.verify(1, getRequestedFor(urlEqualTo("/product-service/api/products/102")));
	}
	
	@Test
	void whenFindAll_andBothServicesDelayResponse_thenEventuallyReturnsAllFavourites() 
			throws JsonProcessingException {
		// Given
		Favourite favourite1 = Favourite.builder()
				.userId(1)
				.productId(101)
				.likeDate(LocalDateTime.now())
				.build();
		
		favouriteRepository.save(favourite1);
		
		UserDto userDto1 = UserDto.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.build();
		
		ProductDto productDto1 = ProductDto.builder()
				.productId(101)
				.productTitle("Laptop")
				.sku("SKU-101")
				.priceUnit(999.99)
				.build();
		
		wireMockServerUser.stubFor(get(urlEqualTo("/user-service/api/users/1"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(userDto1))
						.withFixedDelay(300)));
		
		wireMockServerProduct.stubFor(get(urlEqualTo("/product-service/api/products/101"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(objectMapper.writeValueAsString(productDto1))
						.withFixedDelay(300)));
		
		// When
		long startTime = System.currentTimeMillis();
		List<FavouriteDto> result = favouriteService.findAll();
		long endTime = System.currentTimeMillis();
		
		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getUserDto()).isNotNull();
		assertThat(result.get(0).getProductDto()).isNotNull();
		assertThat(endTime - startTime).isGreaterThanOrEqualTo(600);
		
		wireMockServerUser.verify(1, getRequestedFor(urlEqualTo("/user-service/api/users/1")));
		wireMockServerProduct.verify(1, getRequestedFor(urlEqualTo("/product-service/api/products/101")));
	}
}
