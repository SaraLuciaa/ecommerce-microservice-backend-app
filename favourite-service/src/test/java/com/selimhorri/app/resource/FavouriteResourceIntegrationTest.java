package com.selimhorri.app.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:favourite_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class FavouriteResourceIntegrationTest {

    private static final LocalDateTime EXISTING_LIKE_DATE = LocalDateTime.of(2024, 3, 15, 18, 30, 45, 123_000_000);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    private FavouriteId existingId;

    @BeforeEach
    void setUp() {
        this.favouriteRepository.deleteAll();
        Favourite favourite = Favourite.builder()
                .userId(11)
                .productId(22)
                .likeDate(EXISTING_LIKE_DATE)
                .build();
        this.favouriteRepository.save(favourite);
        this.existingId = new FavouriteId(11, 22, EXISTING_LIKE_DATE);

        when(this.restTemplate.getForObject(userUrl(existingId.getUserId()), UserDto.class))
                .thenReturn(UserDto.builder().userId(existingId.getUserId()).firstName("Alice").build());
        when(this.restTemplate.getForObject(productUrl(existingId.getProductId()), ProductDto.class))
                .thenReturn(ProductDto.builder().productId(existingId.getProductId()).productTitle("Phone").build());
    }

    @Test
    void shouldGetAllFavourites() throws Exception {
        this.mockMvc.perform(get("/api/favourites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].userId", equalTo(existingId.getUserId())))
                .andExpect(jsonPath("$.collection[0].productId", equalTo(existingId.getProductId())))
                .andExpect(jsonPath("$.collection[0].likeDate", equalTo(EXISTING_LIKE_DATE.format(FORMATTER))));
    }

    @Test
    void shouldGetFavouriteByCompositeKey() throws Exception {
        this.mockMvc.perform(get("/api/favourites/{userId}/{productId}/{likeDate}",
                        Integer.toString(existingId.getUserId()),
                        Integer.toString(existingId.getProductId()),
                        EXISTING_LIKE_DATE.format(FORMATTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", equalTo(existingId.getUserId())))
                .andExpect(jsonPath("$.productId", equalTo(existingId.getProductId())))
                .andExpect(jsonPath("$.likeDate", equalTo(EXISTING_LIKE_DATE.format(FORMATTER))));
    }

    @Test
    void shouldCreateFavourite() throws Exception {
        LocalDateTime newLikeDate = EXISTING_LIKE_DATE.plusDays(2);
        FavouriteDto payload = FavouriteDto.builder()
                .userId(33)
                .productId(44)
                .likeDate(newLikeDate)
                .build();

        this.mockMvc.perform(post("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", equalTo(payload.getUserId())))
                .andExpect(jsonPath("$.productId", equalTo(payload.getProductId())))
                .andExpect(jsonPath("$.likeDate", equalTo(newLikeDate.format(FORMATTER))));

        FavouriteId createdId = new FavouriteId(payload.getUserId(), payload.getProductId(), newLikeDate);
        assertThat(this.favouriteRepository.findById(createdId)).isPresent();
    }

    @Test
    void shouldDeleteFavouriteByCompositeKey() throws Exception {
        this.mockMvc.perform(delete("/api/favourites/{userId}/{productId}/{likeDate}",
                        Integer.toString(existingId.getUserId()),
                        Integer.toString(existingId.getProductId()),
                        EXISTING_LIKE_DATE.format(FORMATTER)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertThat(this.favouriteRepository.findById(existingId)).isEmpty();
    }

    private static String userUrl(int userId) {
        return AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
    }

    private static String productUrl(int productId) {
        return AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
    }
}
