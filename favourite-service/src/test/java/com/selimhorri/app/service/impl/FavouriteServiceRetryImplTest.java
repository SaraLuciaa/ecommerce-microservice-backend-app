package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;
import com.selimhorri.app.service.FeatureToggleService;

@SpringBootTest(properties = {
                "resilience4j.retry.instances.favouriteService.max-retry-attempts=3",
                "resilience4j.retry.instances.favouriteService.wait-duration=100ms",
                "resilience4j.retry.instances.favouriteService.enable-exponential-backoff=false",
                "resilience4j.circuitbreaker.circuitBreakerAspectOrder=1",
                "resilience4j.retry.retryAspectOrder=2"
})
class FavouriteServiceRetryImplTest {

        @Autowired
        private FavouriteService favouriteService;

        @MockBean
        private FavouriteRepository favouriteRepository;

        @MockBean
        private RestTemplate restTemplate;

        @MockBean
        private FeatureToggleService featureToggleService;

        private static final LocalDateTime LIKE_DATE = LocalDateTime.of(2023, 1, 1, 10, 15);

        @BeforeEach
        void setUp() {
                when(this.featureToggleService.isFetchDetailsEnabled()).thenReturn(true);
        }

        @Test
        void findAllShouldRetryAndReturnFallbackWhenServiceIsDown() {
                Favourite favourite = Favourite.builder()
                                .userId(1)
                                .productId(1)
                                .likeDate(LIKE_DATE)
                                .build();

                when(this.favouriteRepository.findAll()).thenReturn(List.of(favourite));

                when(this.restTemplate.getForObject(anyString(), eq(UserDto.class)))
                                .thenThrow(new RuntimeException("Service Down"));

                List<FavouriteDto> result = this.favouriteService.findAll();

                assertThat(result).isEmpty();

                verify(this.restTemplate, times(3)).getForObject(anyString(), eq(UserDto.class));
        }

        @Test
        void findByIdShouldRetryAndReturnFallbackWhenServiceIsDown() {
                FavouriteId id = new FavouriteId(1, 1, LIKE_DATE);
                Favourite favourite = Favourite.builder()
                                .userId(1)
                                .productId(1)
                                .likeDate(LIKE_DATE)
                                .build();

                when(this.favouriteRepository.findById(id)).thenReturn(Optional.of(favourite));

                when(this.restTemplate.getForObject(anyString(), eq(UserDto.class)))
                                .thenThrow(new RuntimeException("Service Down"));

                FavouriteDto result = this.favouriteService.findById(id);

                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo(1);
                assertThat(result.getUserDto()).isNull();

                verify(this.restTemplate, times(3)).getForObject(anyString(), eq(UserDto.class));
        }
}
