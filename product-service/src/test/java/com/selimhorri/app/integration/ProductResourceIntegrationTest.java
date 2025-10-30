package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:product_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.config.import=optional:file:./",
        "SPRING_CONFIG_IMPORT=optional:file:./",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "server.servlet.context-path="
})
@AutoConfigureMockMvc
class ProductResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category persistedCategory;
    private Product persistedProduct;

    @BeforeEach
    void setUp() {
        this.productRepository.deleteAll();
        this.categoryRepository.deleteAll();

        this.persistedCategory = this.categoryRepository.save(Category.builder()
                .categoryTitle("Outdoors")
                .imageUrl("outdoors.png")
                .build());

        this.persistedProduct = this.productRepository.save(Product.builder()
                .productTitle("Camping Tent")
                .imageUrl("tent.png")
                .sku("TNT-100")
                .priceUnit(250.0)
                .quantity(12)
                .category(this.persistedCategory)
                .build());
    }

    @Test
    void shouldListProducts() throws Exception {
        this.mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(1)))
                .andExpect(jsonPath("$.collection[0].productId", equalTo(this.persistedProduct.getProductId())))
                .andExpect(jsonPath("$.collection[0].category.categoryId", equalTo(this.persistedCategory.getCategoryId())));
    }

    @Test
    void shouldGetProductById() throws Exception {
        this.mockMvc.perform(get("/api/products/{productId}", this.persistedProduct.getProductId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", equalTo(this.persistedProduct.getProductId())))
                .andExpect(jsonPath("$.category.categoryId", equalTo(this.persistedCategory.getCategoryId())));
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productTitle("Hiking Backpack")
                .imageUrl("backpack.png")
                .sku("BPK-200")
                .priceUnit(180.5)
                .quantity(40)
                .categoryDto(CategoryDto.builder()
                        .categoryId(this.persistedCategory.getCategoryId())
                        .categoryTitle(this.persistedCategory.getCategoryTitle())
                        .imageUrl(this.persistedCategory.getImageUrl())
                        .build())
                .build();

        MvcResult result = this.mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").isNumber())
                .andExpect(jsonPath("$.productTitle", equalTo("Hiking Backpack")))
                .andExpect(jsonPath("$.category.categoryId", equalTo(this.persistedCategory.getCategoryId())))
                .andReturn();

        ProductDto created = this.objectMapper.readValue(result.getResponse().getContentAsByteArray(), ProductDto.class);
        assertThat(this.productRepository.findById(created.getProductId())).isPresent();
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productId(this.persistedProduct.getProductId())
                .productTitle("Four Season Tent")
                .imageUrl("four-season.png")
                .sku(this.persistedProduct.getSku())
                .priceUnit(275.0)
                .quantity(8)
                .categoryDto(CategoryDto.builder()
                        .categoryId(this.persistedCategory.getCategoryId())
                        .categoryTitle(this.persistedCategory.getCategoryTitle())
                        .imageUrl(this.persistedCategory.getImageUrl())
                        .build())
                .build();

        this.mockMvc.perform(put("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", equalTo(this.persistedProduct.getProductId())))
                .andExpect(jsonPath("$.productTitle", equalTo("Four Season Tent")));

        Product reloaded = this.productRepository.findById(this.persistedProduct.getProductId()).orElseThrow();
        assertThat(reloaded.getProductTitle()).isEqualTo("Four Season Tent");
        assertThat(reloaded.getQuantity()).isEqualTo(8);
    }

    @Test
    void shouldPatchProductById() throws Exception {
        ProductDto payload = ProductDto.builder()
                .productTitle("Ultralight Tent")
                .imageUrl("ultralight.png")
                .sku(this.persistedProduct.getSku())
                .priceUnit(310.0)
                .quantity(6)
                .categoryDto(CategoryDto.builder()
                        .categoryId(this.persistedCategory.getCategoryId())
                        .categoryTitle(this.persistedCategory.getCategoryTitle())
                        .imageUrl(this.persistedCategory.getImageUrl())
                        .build())
                .build();

        this.mockMvc.perform(put("/api/products/{productId}", this.persistedProduct.getProductId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", equalTo(this.persistedProduct.getProductId())))
                .andExpect(jsonPath("$.productTitle", equalTo("Ultralight Tent")));

        Product reloaded = this.productRepository.findById(this.persistedProduct.getProductId()).orElseThrow();
        assertThat(reloaded.getProductTitle()).isEqualTo("Ultralight Tent");
        assertThat(reloaded.getPriceUnit()).isEqualTo(310.0);
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        this.mockMvc.perform(delete("/api/products/{productId}", this.persistedProduct.getProductId().toString()))
                .andExpect(status().isOk());

        assertThat(this.productRepository.findById(this.persistedProduct.getProductId())).isEmpty();
    }
}
