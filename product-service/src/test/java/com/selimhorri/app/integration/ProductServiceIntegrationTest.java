package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:product_service_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

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
                .categoryTitle("Audio")
                .imageUrl("audio.png")
                .build());

        this.persistedProduct = this.productRepository.save(Product.builder()
                .productTitle("Bluetooth Speaker")
                .imageUrl("speaker.png")
                .sku("SPK-001")
                .priceUnit(89.99)
                .quantity(25)
                .category(this.persistedCategory)
                .build());
    }

    @Test
    void findAllShouldReturnPersistedProducts() {
        List<ProductDto> products = this.productService.findAll();

        assertThat(products).hasSize(1);
        ProductDto productDto = products.get(0);
        assertThat(productDto.getProductId()).isEqualTo(this.persistedProduct.getProductId());
        assertThat(productDto.getCategoryDto().getCategoryId()).isEqualTo(this.persistedCategory.getCategoryId());
    }

    @Test
    void findByIdShouldReturnProductWhenPresent() {
        ProductDto productDto = this.productService.findById(this.persistedProduct.getProductId());

        assertThat(productDto.getProductId()).isEqualTo(this.persistedProduct.getProductId());
        assertThat(productDto.getProductTitle()).isEqualTo("Bluetooth Speaker");
        assertThat(productDto.getCategoryDto().getCategoryId()).isEqualTo(this.persistedCategory.getCategoryId());
    }

    @Test
    void saveShouldPersistProduct() {
        ProductDto payload = ProductDto.builder()
                .productTitle("Noise Cancelling Headphones")
                .imageUrl("headphones.png")
                .sku("HPH-002")
                .priceUnit(199.5)
                .quantity(15)
                .categoryDto(CategoryDto.builder()
                        .categoryId(this.persistedCategory.getCategoryId())
                        .categoryTitle(this.persistedCategory.getCategoryTitle())
                        .imageUrl(this.persistedCategory.getImageUrl())
                        .build())
                .build();

        ProductDto saved = this.productService.save(payload);

        assertThat(saved.getProductId()).isNotNull();
        Optional<Product> persisted = this.productRepository.findById(saved.getProductId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getSku()).isEqualTo("HPH-002");
    }

    @Test
    void updateShouldModifyExistingProduct() {
        ProductDto payload = ProductDto.builder()
                .productId(this.persistedProduct.getProductId())
                .productTitle("Portable Speaker")
                .imageUrl("portable-speaker.png")
                .sku(this.persistedProduct.getSku())
                .priceUnit(99.99)
                .quantity(30)
                .categoryDto(CategoryDto.builder()
                        .categoryId(this.persistedCategory.getCategoryId())
                        .categoryTitle(this.persistedCategory.getCategoryTitle())
                        .imageUrl(this.persistedCategory.getImageUrl())
                        .build())
                .build();

        ProductDto updated = this.productService.update(payload);

        assertThat(updated.getProductId()).isEqualTo(this.persistedProduct.getProductId());
        Product reloaded = this.productRepository.findById(this.persistedProduct.getProductId()).orElseThrow();
        assertThat(reloaded.getProductTitle()).isEqualTo("Portable Speaker");
        assertThat(reloaded.getQuantity()).isEqualTo(30);
    }

    @Test
    void updateByIdShouldOverrideIdentifier() {
        ProductDto payload = ProductDto.builder()
                .productTitle("Smart Speaker")
                .imageUrl("smart-speaker.png")
                .sku(this.persistedProduct.getSku())
                .priceUnit(129.0)
                .quantity(20)
                .categoryDto(CategoryDto.builder()
                        .categoryId(this.persistedCategory.getCategoryId())
                        .categoryTitle(this.persistedCategory.getCategoryTitle())
                        .imageUrl(this.persistedCategory.getImageUrl())
                        .build())
                .build();

        ProductDto patched = this.productService.update(this.persistedProduct.getProductId(), payload);

        assertThat(patched.getProductId()).isEqualTo(this.persistedProduct.getProductId());
        Product reloaded = this.productRepository.findById(this.persistedProduct.getProductId()).orElseThrow();
        assertThat(reloaded.getProductTitle()).isEqualTo("Smart Speaker");
        assertThat(reloaded.getPriceUnit()).isEqualTo(129.0);
    }

    @Test
    void deleteByIdShouldRemoveProduct() {
        this.productService.deleteById(this.persistedProduct.getProductId());

        assertThat(this.productRepository.findById(this.persistedProduct.getProductId())).isEmpty();
    }
}
