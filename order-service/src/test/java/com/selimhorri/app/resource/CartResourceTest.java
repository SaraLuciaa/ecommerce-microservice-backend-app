package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.CartService;

@WebMvcTest(CartResource.class)
class CartResourceTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CartService cartService;

	private CartDto cartDto;

	@BeforeEach
	void setUp() {
		this.cartDto = CartDto.builder()
				.cartId(1)
				.userId(42)
				.userDto(UserDto.builder().userId(42).firstName("John").lastName("Doe").build())
				.build();
	}

	@Test
	void findAllShouldReturnCartCollection() throws Exception {
		when(this.cartService.findAll()).thenReturn(List.of(this.cartDto));

		this.mockMvc.perform(get("/api/carts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.collection[0].cartId", equalTo(this.cartDto.getCartId())))
				.andExpect(jsonPath("$.collection[0].userId", equalTo(this.cartDto.getUserId())));

		verify(this.cartService).findAll();
	}

	@Test
	void findByIdShouldReturnCart() throws Exception {
		when(this.cartService.findById(1)).thenReturn(this.cartDto);

		this.mockMvc.perform(get("/api/carts/{cartId}", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId", equalTo(this.cartDto.getCartId())))
				.andExpect(jsonPath("$.userId", equalTo(this.cartDto.getUserId())));

		verify(this.cartService).findById(1);
	}

	@Test
	void saveShouldDelegateToService() throws Exception {
		CartDto payload = CartDto.builder().userId(77).build();
		when(this.cartService.save(any(CartDto.class))).thenReturn(this.cartDto);

		this.mockMvc.perform(post("/api/carts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId", equalTo(this.cartDto.getCartId())));

		ArgumentCaptor<CartDto> captor = ArgumentCaptor.forClass(CartDto.class);
		verify(this.cartService).save(captor.capture());
		CartDto sent = captor.getValue();
		org.assertj.core.api.Assertions.assertThat(sent.getUserId()).isEqualTo(payload.getUserId());
	}

	@Test
	void updateShouldDelegateToService() throws Exception {
		CartDto payload = CartDto.builder().cartId(1).userId(90).build();
		when(this.cartService.update(any(CartDto.class))).thenReturn(payload);

		this.mockMvc.perform(put("/api/carts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId", equalTo(payload.getCartId())))
				.andExpect(jsonPath("$.userId", equalTo(payload.getUserId())));

		ArgumentCaptor<CartDto> captor = ArgumentCaptor.forClass(CartDto.class);
		verify(this.cartService).update(captor.capture());
		CartDto sent = captor.getValue();
		org.assertj.core.api.Assertions.assertThat(sent.getCartId()).isEqualTo(payload.getCartId());
		org.assertj.core.api.Assertions.assertThat(sent.getUserId()).isEqualTo(payload.getUserId());
	}

	@Test
	void updateByIdShouldParsePathVariableAndDelegate() throws Exception {
		CartDto payload = CartDto.builder().userId(55).build();
		when(this.cartService.update(anyInt(), any(CartDto.class))).thenReturn(this.cartDto);

		this.mockMvc.perform(put("/api/carts/{cartId}", "5")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId", equalTo(this.cartDto.getCartId())));

		ArgumentCaptor<CartDto> captor = ArgumentCaptor.forClass(CartDto.class);
		verify(this.cartService).update(org.mockito.ArgumentMatchers.eq(5), captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().getUserId()).isEqualTo(payload.getUserId());
	}

	@Test
	void deleteByIdShouldReturnTrue() throws Exception {
		this.mockMvc.perform(delete("/api/carts/{cartId}", "4"))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		verify(this.cartService).deleteById(4);
	}
}
