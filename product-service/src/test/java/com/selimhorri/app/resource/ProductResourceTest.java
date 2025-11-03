package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.service.ProductService;

@WebMvcTest(ProductResource.class)
class ProductResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        this.productDto = ProductDto.builder()
                .productId(5)
                .productTitle("Phone")
                .imageUrl("phone.png")
                .sku("PH-001")
                .priceUnit(799.0)
                .quantity(10)
                .categoryDto(CategoryDto.builder()
                        .categoryId(2)
                        .categoryTitle("Electronics")
                        .imageUrl("electronics.png")
                        .build())
                .build();
    }

    @Test
    void findAllShouldReturnCollectionResponse() throws Exception {
        when(this.productService.findAll()).thenReturn(List.of(this.productDto));

        this.mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].productId", equalTo(this.productDto.getProductId())));

        verify(this.productService).findAll();
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        when(this.productService.findById(5)).thenReturn(this.productDto);

        this.mockMvc.perform(get("/api/products/{productId}", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", equalTo(this.productDto.getSku())));

        verify(this.productService).findById(5);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.productService.save(any(ProductDto.class))).thenReturn(this.productDto);

        this.mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.productDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle", equalTo(this.productDto.getProductTitle())));

        ArgumentCaptor<ProductDto> captor = ArgumentCaptor.forClass(ProductDto.class);
        verify(this.productService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getCategoryDto().getCategoryId()).isEqualTo(this.productDto.getCategoryDto().getCategoryId());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.productService.update(any(ProductDto.class))).thenReturn(this.productDto);

        this.mockMvc.perform(put("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.productDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", equalTo(this.productDto.getQuantity())));

        verify(this.productService).update(any(ProductDto.class));
    }

    @Test
    void updateWithIdShouldPassParsedIdentifier() throws Exception {
        when(this.productService.update(eq(5), any(ProductDto.class))).thenReturn(this.productDto);

        this.mockMvc.perform(put("/api/products/{productId}", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.productDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle", equalTo(this.productDto.getProductTitle())));

        ArgumentCaptor<ProductDto> captor = ArgumentCaptor.forClass(ProductDto.class);
        verify(this.productService).update(eq(5), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getSku()).isEqualTo(this.productDto.getSku());
    }

    @Test
    void deleteShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/products/{productId}", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.productService).deleteById(9);
    }
}
