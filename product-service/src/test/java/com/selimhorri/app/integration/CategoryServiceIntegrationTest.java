package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.service.CategoryService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:product_category_service_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category parentCategory;
    private Category persistedCategory;

    @BeforeEach
    void setUp() {
        this.categoryRepository.deleteAll();
        this.parentCategory = this.categoryRepository.save(Category.builder()
                .categoryTitle("Electronics")
                .imageUrl("electronics.png")
                .build());
        this.persistedCategory = this.categoryRepository.save(Category.builder()
                .categoryTitle("Cameras")
                .imageUrl("cameras.png")
                .parentCategory(this.parentCategory)
                .build());
    }

    @Test
    void findAllShouldReturnPersistedCategories() {
        List<CategoryDto> categories = this.categoryService.findAll();

        assertThat(categories).hasSize(2);
        assertThat(categories)
                .extracting(CategoryDto::getCategoryTitle)
                .contains("Electronics", "Cameras");
    }

    @Test
    void findByIdShouldReturnCategoryWhenPresent() {
        CategoryDto categoryDto = this.categoryService.findById(this.persistedCategory.getCategoryId());

        assertThat(categoryDto.getCategoryId()).isEqualTo(this.persistedCategory.getCategoryId());
        assertThat(categoryDto.getCategoryTitle()).isEqualTo("Cameras");
        assertThat(categoryDto.getParentCategoryDto().getCategoryId()).isEqualTo(this.parentCategory.getCategoryId());
    }

    @Test
    void saveShouldPersistCategory() {
        CategoryDto payload = CategoryDto.builder()
                .categoryTitle("Lenses")
                .imageUrl("lenses.png")
                .parentCategoryDto(CategoryDto.builder()
                        .categoryId(this.parentCategory.getCategoryId())
                        .categoryTitle(this.parentCategory.getCategoryTitle())
                        .imageUrl(this.parentCategory.getImageUrl())
                        .build())
                .build();

        CategoryDto saved = this.categoryService.save(payload);

        assertThat(saved.getCategoryId()).isNotNull();
        Optional<Category> persisted = this.categoryRepository.findById(saved.getCategoryId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getParentCategory().getCategoryId()).isEqualTo(this.parentCategory.getCategoryId());
    }

    @Test
    void updateShouldModifyExistingCategory() {
        CategoryDto payload = CategoryDto.builder()
                .categoryId(this.persistedCategory.getCategoryId())
                .categoryTitle("Mirrorless Cameras")
                .imageUrl("mirrorless.png")
                .parentCategoryDto(CategoryDto.builder()
                        .categoryId(this.parentCategory.getCategoryId())
                        .categoryTitle(this.parentCategory.getCategoryTitle())
                        .imageUrl(this.parentCategory.getImageUrl())
                        .build())
                .build();

        CategoryDto updated = this.categoryService.update(payload);

        assertThat(updated.getCategoryId()).isEqualTo(this.persistedCategory.getCategoryId());
        Category reloaded = this.categoryRepository.findById(this.persistedCategory.getCategoryId()).orElseThrow();
        assertThat(reloaded.getCategoryTitle()).isEqualTo("Mirrorless Cameras");
        assertThat(reloaded.getImageUrl()).isEqualTo("mirrorless.png");
    }

    @Test
    void updateByIdShouldOverrideIdentifier() {
        CategoryDto payload = CategoryDto.builder()
                .categoryTitle("Photography")
                .imageUrl("photography.png")
                .parentCategoryDto(CategoryDto.builder()
                        .categoryId(this.parentCategory.getCategoryId())
                        .categoryTitle(this.parentCategory.getCategoryTitle())
                        .imageUrl(this.parentCategory.getImageUrl())
                        .build())
                .build();

        CategoryDto patched = this.categoryService.update(this.persistedCategory.getCategoryId(), payload);

        assertThat(patched.getCategoryId()).isEqualTo(this.persistedCategory.getCategoryId());
        Category reloaded = this.categoryRepository.findById(this.persistedCategory.getCategoryId()).orElseThrow();
        assertThat(reloaded.getCategoryTitle()).isEqualTo("Photography");
        assertThat(reloaded.getImageUrl()).isEqualTo("photography.png");
    }

    @Test
    void deleteByIdShouldRemoveCategory() {
        this.categoryService.deleteById(this.persistedCategory.getCategoryId());

        assertThat(this.categoryRepository.findById(this.persistedCategory.getCategoryId())).isEmpty();
        assertThat(this.categoryRepository.findAll()).hasSize(1);
    }
}
