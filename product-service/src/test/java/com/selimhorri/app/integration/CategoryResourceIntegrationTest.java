package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
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
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.repository.CategoryRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:product_category_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class CategoryResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category parentCategory;
    private Category persistedCategory;

    @BeforeEach
    void setUp() {
        this.categoryRepository.deleteAll();
        this.parentCategory = this.categoryRepository.save(Category.builder()
                .categoryTitle("Home")
                .imageUrl("home.png")
                .build());
        this.persistedCategory = this.categoryRepository.save(Category.builder()
                .categoryTitle("Kitchen")
                .imageUrl("kitchen.png")
                .parentCategory(this.parentCategory)
                .build());
    }

    @Test
    void shouldListCategories() throws Exception {
        this.mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(2)))
                .andExpect(jsonPath("$.collection[*].categoryId",
                        hasItems(this.parentCategory.getCategoryId(), this.persistedCategory.getCategoryId())));
    }

    @Test
    void shouldGetCategoryById() throws Exception {
        this.mockMvc.perform(get("/api/categories/{categoryId}", this.persistedCategory.getCategoryId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", equalTo(this.persistedCategory.getCategoryId())))
                .andExpect(jsonPath("$.parentCategory.categoryId", equalTo(this.parentCategory.getCategoryId())));
    }

    @Test
    void shouldCreateCategory() throws Exception {
        CategoryDto payload = CategoryDto.builder()
                .categoryTitle("Appliances")
                .imageUrl("appliances.png")
                .parentCategoryDto(CategoryDto.builder()
                        .categoryId(this.parentCategory.getCategoryId())
                        .build())
                .build();

        MvcResult result = this.mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").isNumber())
                .andExpect(jsonPath("$.categoryTitle", equalTo("Appliances")))
                .andExpect(jsonPath("$.parentCategory.categoryId", equalTo(this.parentCategory.getCategoryId())))
                .andReturn();

        CategoryDto created = this.objectMapper.readValue(result.getResponse().getContentAsByteArray(), CategoryDto.class);
        assertThat(this.categoryRepository.findById(created.getCategoryId())).isPresent();
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        CategoryDto payload = CategoryDto.builder()
                .categoryId(this.persistedCategory.getCategoryId())
                .categoryTitle("Cookware")
                .imageUrl("cookware.png")
                .parentCategoryDto(CategoryDto.builder()
                        .categoryId(this.parentCategory.getCategoryId())
                        .build())
                .build();

        this.mockMvc.perform(put("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", equalTo(this.persistedCategory.getCategoryId())))
                .andExpect(jsonPath("$.categoryTitle", equalTo("Cookware")));

        Category reloaded = this.categoryRepository.findById(this.persistedCategory.getCategoryId()).orElseThrow();
        assertThat(reloaded.getCategoryTitle()).isEqualTo("Cookware");
    }

    @Test
    void shouldPatchCategoryById() throws Exception {
        CategoryDto payload = CategoryDto.builder()
                .categoryTitle("Dining")
                .imageUrl("dining.png")
                .parentCategoryDto(CategoryDto.builder()
                        .categoryId(this.parentCategory.getCategoryId())
                        .build())
                .build();

        this.mockMvc.perform(put("/api/categories/{categoryId}", this.persistedCategory.getCategoryId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", equalTo(this.persistedCategory.getCategoryId())))
                .andExpect(jsonPath("$.categoryTitle", equalTo("Dining")));

        Category reloaded = this.categoryRepository.findById(this.persistedCategory.getCategoryId()).orElseThrow();
        assertThat(reloaded.getCategoryTitle()).isEqualTo("Dining");
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        this.mockMvc.perform(delete("/api/categories/{categoryId}", this.persistedCategory.getCategoryId().toString()))
                .andExpect(status().isOk());

        assertThat(this.categoryRepository.findById(this.persistedCategory.getCategoryId())).isEmpty();
    }
}
