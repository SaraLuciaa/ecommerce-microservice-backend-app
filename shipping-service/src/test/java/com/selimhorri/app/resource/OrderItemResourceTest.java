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
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.service.OrderItemService;

@WebMvcTest(OrderItemResource.class)
class OrderItemResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderItemService orderItemService;

    private OrderItemDto orderItemDto;

    @BeforeEach
    void setUp() {
        this.orderItemDto = OrderItemDto.builder()
                .productId(1)
                .orderId(2)
                .orderedQuantity(5)
                .build();
    }

    @Test
    void findAllShouldReturnCollection() throws Exception {
        when(this.orderItemService.findAll()).thenReturn(List.of(this.orderItemDto));

        this.mockMvc.perform(get("/api/shippings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].productId", equalTo(this.orderItemDto.getProductId())))
                .andExpect(jsonPath("$.collection[0].orderId", equalTo(this.orderItemDto.getOrderId())));

        verify(this.orderItemService).findAll();
    }

    @Test
    void findByCompositePathShouldReturnDto() throws Exception {
        OrderItemId orderItemId = new OrderItemId(1, 2);
        when(this.orderItemService.findById(orderItemId)).thenReturn(this.orderItemDto);

        this.mockMvc.perform(get("/api/shippings/{orderId}/{productId}", Integer.toString(orderItemId.getOrderId()), Integer.toString(orderItemId.getProductId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", equalTo(this.orderItemDto.getProductId())));

        verify(this.orderItemService).findById(orderItemId);
    }

    @Test
    void findByBodyShouldReturnDto() throws Exception {
        OrderItemId orderItemId = new OrderItemId(3, 4);
        when(this.orderItemService.findById(orderItemId)).thenReturn(this.orderItemDto);

        this.mockMvc.perform(get("/api/shippings/find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(orderItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", equalTo(this.orderItemDto.getOrderId())));

        verify(this.orderItemService).findById(orderItemId);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.orderItemService.save(any(OrderItemDto.class))).thenReturn(this.orderItemDto);

        this.mockMvc.perform(post("/api/shippings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.orderItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", equalTo(this.orderItemDto.getProductId())));

        ArgumentCaptor<OrderItemDto> captor = ArgumentCaptor.forClass(OrderItemDto.class);
        verify(this.orderItemService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getOrderedQuantity()).isEqualTo(this.orderItemDto.getOrderedQuantity());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.orderItemService.update(any(OrderItemDto.class))).thenReturn(this.orderItemDto);

        this.mockMvc.perform(put("/api/shippings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.orderItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", equalTo(this.orderItemDto.getOrderId())));

        ArgumentCaptor<OrderItemDto> captor = ArgumentCaptor.forClass(OrderItemDto.class);
        verify(this.orderItemService).update(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getProductId()).isEqualTo(this.orderItemDto.getProductId());
    }

    @Test
    void deleteByCompositePathShouldReturnTrue() throws Exception {
        OrderItemId orderItemId = new OrderItemId(6, 7);

        this.mockMvc.perform(delete("/api/shippings/{orderId}/{productId}", Integer.toString(orderItemId.getOrderId()), Integer.toString(orderItemId.getProductId())))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.orderItemService).deleteById(orderItemId);
    }

    @Test
    void deleteByBodyShouldReturnTrue() throws Exception {
        OrderItemId orderItemId = new OrderItemId(8, 9);

        this.mockMvc.perform(delete("/api/shippings/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(orderItemId)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.orderItemService).deleteById(orderItemId);
    }
}
