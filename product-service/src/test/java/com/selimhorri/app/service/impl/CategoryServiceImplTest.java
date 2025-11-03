package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.wrapper.CategoryNotFoundException;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        this.categoryService = new CategoryServiceImpl(this.categoryRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        Category root = buildCategory(1, "Root", null);
        when(this.categoryRepository.findAll()).thenReturn(List.of(root));

        List<CategoryDto> result = this.categoryService.findAll();

        assertThat(result).hasSize(1);
        CategoryDto dto = result.get(0);
        assertThat(dto.getCategoryId()).isEqualTo(root.getCategoryId());
        assertThat(dto.getCategoryTitle()).isEqualTo(root.getCategoryTitle());

        verify(this.categoryRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        Category category = buildCategory(2, "Phones", buildCategory(99, "Electronics", null));
        when(this.categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));

        CategoryDto result = this.categoryService.findById(category.getCategoryId());

        assertThat(result.getCategoryId()).isEqualTo(category.getCategoryId());
        assertThat(result.getParentCategoryDto().getCategoryId()).isEqualTo(category.getParentCategory().getCategoryId());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.categoryRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> this.categoryService.findById(404));
        verify(this.categoryRepository).findById(404);
    }

    @Test
    void saveShouldPersistMappedEntity() {
        CategoryDto payload = buildCategoryDto(null, "New", 10, "Parent");
        Category persisted = buildCategory(3, payload.getCategoryTitle(), buildCategory(10, "Parent", null));
        when(this.categoryRepository.save(any(Category.class))).thenReturn(persisted);

        CategoryDto result = this.categoryService.save(payload);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(this.categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertThat(saved.getCategoryId()).isNull();
        assertThat(saved.getCategoryTitle()).isEqualTo(payload.getCategoryTitle());
        assertThat(saved.getParentCategory().getCategoryId()).isEqualTo(payload.getParentCategoryDto().getCategoryId());

        assertThat(result.getCategoryId()).isEqualTo(persisted.getCategoryId());
    }

    @Test
    void updateShouldPersistMappedEntity() {
        CategoryDto payload = buildCategoryDto(5, "Updated", 10, "Parent");
        Category persisted = buildCategory(payload.getCategoryId(), payload.getCategoryTitle(), buildCategory(10, "Parent", null));
        when(this.categoryRepository.save(any(Category.class))).thenReturn(persisted);

        CategoryDto result = this.categoryService.update(payload);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(this.categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertThat(saved.getCategoryId()).isEqualTo(payload.getCategoryId());
        assertThat(saved.getCategoryTitle()).isEqualTo(payload.getCategoryTitle());

        assertThat(result.getCategoryTitle()).isEqualTo(payload.getCategoryTitle());
    }

    @Test
    void updateWithIdShouldApplyNewValues() {
        Category existing = buildCategory(8, "Legacy", buildCategory(1, "Root", null));
        when(this.categoryRepository.findById(existing.getCategoryId())).thenReturn(Optional.of(existing));
        CategoryDto payload = buildCategoryDto(null, "Modern", 2, "Root-2");
        Category persisted = buildCategory(existing.getCategoryId(), payload.getCategoryTitle(), buildCategory(2, "Root-2", null));
        when(this.categoryRepository.save(any(Category.class))).thenReturn(persisted);

        CategoryDto result = this.categoryService.update(existing.getCategoryId(), payload);

        verify(this.categoryRepository).findById(existing.getCategoryId());
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(this.categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertThat(saved.getCategoryId()).isEqualTo(existing.getCategoryId());
        assertThat(saved.getCategoryTitle()).isEqualTo(payload.getCategoryTitle());
        assertThat(saved.getParentCategory().getCategoryId()).isEqualTo(payload.getParentCategoryDto().getCategoryId());

        assertThat(result.getCategoryId()).isEqualTo(existing.getCategoryId());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.categoryService.deleteById(33);

        verify(this.categoryRepository).deleteById(33);
    }

    private static Category buildCategory(Integer id, String title, Category parent) {
        return Category.builder()
                .categoryId(id)
                .categoryTitle(title)
                .imageUrl(title == null ? null : title.toLowerCase() + ".png")
                .parentCategory(parent)
                .subCategories(Set.of())
                .products(Set.of())
                .build();
    }

    private static CategoryDto buildCategoryDto(Integer id, String title, Integer parentId, String parentTitle) {
        CategoryDto parent = CategoryDto.builder()
                .categoryId(parentId)
                .categoryTitle(parentTitle)
                .imageUrl(parentTitle == null ? null : parentTitle.toLowerCase() + ".png")
                .build();

        return CategoryDto.builder()
                .categoryId(id)
                .categoryTitle(title)
                .imageUrl(title == null ? null : title.toLowerCase() + ".png")
                .parentCategoryDto(parent)
                .build();
    }
}
