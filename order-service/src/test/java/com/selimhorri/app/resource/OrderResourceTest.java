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
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.service.OrderService;

@WebMvcTest(OrderResource.class)
class OrderResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
	this.orderDto = OrderDto.builder()
		.orderId(10)
		.orderDesc("sample")
		.orderFee(99.5)
		.cartDto(CartDto.builder().cartId(3).build())
		.build();
    }

    @Test
    void findAllShouldReturnOrderCollection() throws Exception {
	when(this.orderService.findAll()).thenReturn(List.of(this.orderDto));

	this.mockMvc.perform(get("/api/orders"))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.collection[0].orderId", equalTo(this.orderDto.getOrderId())))
		.andExpect(jsonPath("$.collection[0].orderDesc", equalTo(this.orderDto.getOrderDesc())));

	verify(this.orderService).findAll();
    }

    @Test
    void findByIdShouldReturnOrder() throws Exception {
	when(this.orderService.findById(10)).thenReturn(this.orderDto);

	this.mockMvc.perform(get("/api/orders/{orderId}", "10"))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.orderId", equalTo(this.orderDto.getOrderId())))
		.andExpect(jsonPath("$.orderDesc", equalTo(this.orderDto.getOrderDesc())));

	verify(this.orderService).findById(10);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
	OrderDto payload = OrderDto.builder()
		.orderDesc("new-order")
		.orderFee(45.0)
		.cartDto(CartDto.builder().cartId(5).build())
		.build();
	when(this.orderService.save(any(OrderDto.class))).thenReturn(this.orderDto);

	this.mockMvc.perform(post("/api/orders")
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.objectMapper.writeValueAsString(payload)))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.orderId", equalTo(this.orderDto.getOrderId())));

	ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
	verify(this.orderService).save(captor.capture());
	org.assertj.core.api.Assertions.assertThat(captor.getValue().getOrderDesc()).isEqualTo(payload.getOrderDesc());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
	when(this.orderService.update(any(OrderDto.class))).thenReturn(this.orderDto);

	this.mockMvc.perform(put("/api/orders")
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.objectMapper.writeValueAsString(this.orderDto)))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.orderId", equalTo(this.orderDto.getOrderId())));

	ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
	verify(this.orderService).update(captor.capture());
	org.assertj.core.api.Assertions.assertThat(captor.getValue().getOrderId()).isEqualTo(this.orderDto.getOrderId());
    }

    @Test
    void updateByIdShouldParseAndDelegate() throws Exception {
	OrderDto payload = OrderDto.builder()
		.orderDesc("patched")
		.orderFee(12.0)
		.cartDto(CartDto.builder().cartId(9).build())
		.build();
	when(this.orderService.update(anyInt(), any(OrderDto.class))).thenReturn(this.orderDto);

	this.mockMvc.perform(put("/api/orders/{orderId}", "10")
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.objectMapper.writeValueAsString(payload)))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.orderId", equalTo(this.orderDto.getOrderId())));

	ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
	verify(this.orderService).update(org.mockito.ArgumentMatchers.eq(10), captor.capture());
	org.assertj.core.api.Assertions.assertThat(captor.getValue().getOrderDesc()).isEqualTo(payload.getOrderDesc());
    }

    @Test
    void deleteByIdShouldReturnTrue() throws Exception {
	this.mockMvc.perform(delete("/api/orders/{orderId}", "8"))
		.andExpect(status().isOk())
		.andExpect(content().string("true"));

	verify(this.orderService).deleteById(8);
    }
}
