package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:favourite_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class FavouriteServiceIntegrationTest {

    private static final LocalDateTime EXISTING_LIKE_DATE = LocalDateTime.of(2024, 1, 15, 10, 15, 30, 123_000_000);

    @Autowired
    private FavouriteService favouriteService;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    private FavouriteId existingId;

    @BeforeEach
    void setUp() {
        this.favouriteRepository.deleteAll();
        Favourite favourite = Favourite.builder()
                .userId(101)
                .productId(202)
                .likeDate(EXISTING_LIKE_DATE)
                .build();
        this.favouriteRepository.save(favourite);
        this.existingId = new FavouriteId(101, 202, EXISTING_LIKE_DATE);
    }

    @Test
    void findAllShouldReturnPersistedFavouritesWithRemoteDetails() {
        mockRemoteUser(existingId.getUserId(), "Alice");
        mockRemoteProduct(existingId.getProductId(), "Camera");

        List<FavouriteDto> favourites = this.favouriteService.findAll();

        assertThat(favourites).hasSize(1);
        FavouriteDto favouriteDto = favourites.get(0);
        assertThat(favouriteDto.getUserId()).isEqualTo(existingId.getUserId());
        assertThat(favouriteDto.getProductId()).isEqualTo(existingId.getProductId());
        assertThat(favouriteDto.getLikeDate()).isEqualTo(existingId.getLikeDate());
        assertThat(favouriteDto.getUserDto().getFirstName()).isEqualTo("Alice");
        assertThat(favouriteDto.getProductDto().getProductTitle()).isEqualTo("Camera");

        verify(this.restTemplate).getForObject(userUrl(existingId.getUserId()), UserDto.class);
        verify(this.restTemplate).getForObject(productUrl(existingId.getProductId()), ProductDto.class);
    }

    @Test
    void findByIdShouldReturnFavouriteWhenPresent() {
        mockRemoteUser(existingId.getUserId(), "Bob");
        mockRemoteProduct(existingId.getProductId(), "Laptop");

        FavouriteDto favouriteDto = this.favouriteService.findById(existingId);

        assertThat(favouriteDto.getUserId()).isEqualTo(existingId.getUserId());
        assertThat(favouriteDto.getProductId()).isEqualTo(existingId.getProductId());
        assertThat(favouriteDto.getLikeDate()).isEqualTo(existingId.getLikeDate());
        assertThat(favouriteDto.getUserDto().getFirstName()).isEqualTo("Bob");
        assertThat(favouriteDto.getProductDto().getProductTitle()).isEqualTo("Laptop");
    }

    @Test
    void saveShouldPersistFavourite() {
        FavouriteDto payload = FavouriteDto.builder()
                .userId(303)
                .productId(404)
                .likeDate(EXISTING_LIKE_DATE.plusDays(1))
                .build();

        FavouriteDto saved = this.favouriteService.save(payload);

        Optional<Favourite> persisted = this.favouriteRepository.findById(new FavouriteId(303, 404, payload.getLikeDate()));
        assertThat(persisted).isPresent();
        assertThat(saved.getUserId()).isEqualTo(payload.getUserId());
        assertThat(saved.getProductId()).isEqualTo(payload.getProductId());
    }

    @Test
    void deleteByIdShouldRemoveFavourite() {
        this.favouriteService.deleteById(existingId);

        assertThat(this.favouriteRepository.findAll()).isEmpty();
    }

    private void mockRemoteUser(int userId, String firstName) {
        when(this.restTemplate.getForObject(userUrl(userId), UserDto.class))
                .thenReturn(UserDto.builder().userId(userId).firstName(firstName).build());
    }

    private void mockRemoteProduct(int productId, String title) {
        when(this.restTemplate.getForObject(productUrl(productId), ProductDto.class))
                .thenReturn(ProductDto.builder().productId(productId).productTitle(title).build());
    }

    private static String userUrl(int userId) {
        return AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
    }

    private static String productUrl(int productId) {
        return AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
    }
}
