package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        this.productService = new ProductServiceImpl(this.productRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        Product product = buildProduct(1, "Phone", "PH-01", 750.0, 10, buildCategory(9, "Electronics"));
        when(this.productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDto> result = this.productService.findAll();

        assertThat(result).hasSize(1);
        ProductDto dto = result.get(0);
        assertThat(dto.getProductId()).isEqualTo(product.getProductId());
        assertThat(dto.getCategoryDto().getCategoryId()).isEqualTo(product.getCategory().getCategoryId());

        verify(this.productRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        Product product = buildProduct(2, "Laptop", "LP-02", 1500.0, 5, buildCategory(9, "Electronics"));
        when(this.productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));

        ProductDto result = this.productService.findById(product.getProductId());

        assertThat(result.getProductId()).isEqualTo(product.getProductId());
        assertThat(result.getSku()).isEqualTo(product.getSku());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.productRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> this.productService.findById(404));
        verify(this.productRepository).findById(404);
    }

    @Test
    void saveShouldPersistMappedEntity() {
        ProductDto payload = buildProductDto(null, "Camera", "CM-03", 899.99, 7, 3);
        Product persisted = buildProduct(3, payload.getProductTitle(), payload.getSku(), payload.getPriceUnit(), payload.getQuantity(), buildCategory(3, "Photo"));
        when(this.productRepository.save(any(Product.class))).thenReturn(persisted);

        ProductDto result = this.productService.save(payload);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(this.productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductId()).isNull();
        assertThat(saved.getSku()).isEqualTo(payload.getSku());
        assertThat(saved.getCategory().getCategoryId()).isEqualTo(payload.getCategoryDto().getCategoryId());

        assertThat(result.getProductId()).isEqualTo(persisted.getProductId());
    }

    @Test
    void updateShouldPersistMappedEntity() {
        ProductDto payload = buildProductDto(5, "Mouse", "MS-05", 49.99, 30, 4);
        Product persisted = buildProduct(payload.getProductId(), payload.getProductTitle(), payload.getSku(), payload.getPriceUnit(), payload.getQuantity(), buildCategory(4, "Accessories"));
        when(this.productRepository.save(any(Product.class))).thenReturn(persisted);

        ProductDto result = this.productService.update(payload);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(this.productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductId()).isEqualTo(payload.getProductId());
        assertThat(saved.getSku()).isEqualTo(payload.getSku());

        assertThat(result.getPriceUnit()).isEqualTo(payload.getPriceUnit());
    }

    @Test
    void updateWithIdShouldApplyNewValues() {
        Product existing = buildProduct(7, "Legacy", "LG-07", 100.0, 1, buildCategory(2, "Other"));
        when(this.productRepository.findById(existing.getProductId())).thenReturn(Optional.of(existing));
        ProductDto payload = buildProductDto(null, "Modern", "MD-07", 120.0, 3, 9);
        Product persisted = buildProduct(existing.getProductId(), payload.getProductTitle(), payload.getSku(), payload.getPriceUnit(), payload.getQuantity(), buildCategory(9, "Updated"));
        when(this.productRepository.save(any(Product.class))).thenReturn(persisted);

        ProductDto result = this.productService.update(existing.getProductId(), payload);

        verify(this.productRepository).findById(existing.getProductId());
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(this.productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductId()).isEqualTo(existing.getProductId());
        assertThat(saved.getSku()).isEqualTo(payload.getSku());
        assertThat(saved.getCategory().getCategoryId()).isEqualTo(payload.getCategoryDto().getCategoryId());

        assertThat(result.getProductId()).isEqualTo(existing.getProductId());
    }

    @Test
    void deleteByIdShouldFetchAndDeleteEntity() {
        Product existing = buildProduct(11, "Delete", "DL-11", 10.0, 2, buildCategory(1, "Misc"));
        when(this.productRepository.findById(existing.getProductId())).thenReturn(Optional.of(existing));

        this.productService.deleteById(existing.getProductId());

        verify(this.productRepository).findById(existing.getProductId());
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(this.productRepository).delete(captor.capture());
        Product deleted = captor.getValue();
        assertThat(deleted.getProductId()).isEqualTo(existing.getProductId());
        assertThat(deleted.getSku()).isEqualTo(existing.getSku());
    }

    private static Product buildProduct(Integer id, String title, String sku, Double price, Integer quantity, Category category) {
        return Product.builder()
                .productId(id)
                .productTitle(title)
                .imageUrl(title == null ? null : title.toLowerCase() + ".png")
                .sku(sku)
                .priceUnit(price)
                .quantity(quantity)
                .category(category)
                .build();
    }

    private static ProductDto buildProductDto(Integer id, String title, String sku, Double price, Integer quantity, Integer categoryId) {
        return ProductDto.builder()
                .productId(id)
                .productTitle(title)
                .imageUrl(title == null ? null : title.toLowerCase() + ".png")
                .sku(sku)
                .priceUnit(price)
                .quantity(quantity)
                .categoryDto(CategoryDto.builder()
                        .categoryId(categoryId)
                        .categoryTitle("Category-" + categoryId)
                        .imageUrl("cat" + categoryId + ".png")
                        .build())
                .build();
    }

    private static Category buildCategory(Integer id, String title) {
        return Category.builder()
                .categoryId(id)
                .categoryTitle(title)
                .imageUrl(title == null ? null : title.toLowerCase() + ".png")
                .build();
    }
}
