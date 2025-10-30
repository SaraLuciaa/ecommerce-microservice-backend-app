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
import com.selimhorri.app.service.CategoryService;

@WebMvcTest(CategoryResource.class)
class CategoryResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        this.categoryDto = CategoryDto.builder()
                .categoryId(3)
                .categoryTitle("Electronics")
                .imageUrl("electronics.png")
                .build();
    }

    @Test
    void findAllShouldReturnCollectionResponse() throws Exception {
        when(this.categoryService.findAll()).thenReturn(List.of(this.categoryDto));

        this.mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].categoryId", equalTo(this.categoryDto.getCategoryId())));

        verify(this.categoryService).findAll();
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        when(this.categoryService.findById(3)).thenReturn(this.categoryDto);

        this.mockMvc.perform(get("/api/categories/{categoryId}", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryTitle", equalTo(this.categoryDto.getCategoryTitle())));

        verify(this.categoryService).findById(3);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.categoryService.save(any(CategoryDto.class))).thenReturn(this.categoryDto);

        this.mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.categoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", equalTo(this.categoryDto.getCategoryId())));

        ArgumentCaptor<CategoryDto> captor = ArgumentCaptor.forClass(CategoryDto.class);
        verify(this.categoryService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getCategoryTitle()).isEqualTo(this.categoryDto.getCategoryTitle());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.categoryService.update(any(CategoryDto.class))).thenReturn(this.categoryDto);

        this.mockMvc.perform(put("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.categoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", equalTo(this.categoryDto.getImageUrl())));

        verify(this.categoryService).update(any(CategoryDto.class));
    }

    @Test
    void updateWithIdShouldPassParsedIdentifier() throws Exception {
        when(this.categoryService.update(eq(3), any(CategoryDto.class))).thenReturn(this.categoryDto);

        this.mockMvc.perform(put("/api/categories/{categoryId}", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.categoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryTitle", equalTo(this.categoryDto.getCategoryTitle())));

        ArgumentCaptor<CategoryDto> captor = ArgumentCaptor.forClass(CategoryDto.class);
        verify(this.categoryService).update(eq(3), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getImageUrl()).isEqualTo(this.categoryDto.getImageUrl());
    }

    @Test
    void deleteShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/categories/{categoryId}", "4"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.categoryService).deleteById(4);
    }
}
