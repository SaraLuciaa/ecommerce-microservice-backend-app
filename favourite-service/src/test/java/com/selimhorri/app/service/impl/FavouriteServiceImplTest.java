package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;
import com.selimhorri.app.service.FeatureToggleService;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceImplTest {

    private static final LocalDateTime LIKE_DATE = LocalDateTime.of(2023, 1, 1, 10, 15);

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FeatureToggleService featureToggleService;

    private FavouriteService favouriteService;

    @BeforeEach
    void setUp() {
        this.favouriteService = new FavouriteServiceImpl(this.favouriteRepository, this.restTemplate,
                this.featureToggleService);
    }

    @Test
    void findAllShouldReturnMappedDtosWithRemoteDetails() {
        Favourite favourite = buildFavourite(11, 22, LIKE_DATE);
        when(this.favouriteRepository.findAll()).thenReturn(List.of(favourite));
        when(this.featureToggleService.isFetchDetailsEnabled()).thenReturn(true);
        when(this.restTemplate.getForObject(userUrl(favourite.getUserId()), UserDto.class))
                .thenReturn(UserDto.builder().userId(favourite.getUserId()).firstName("Alice").build());
        when(this.restTemplate.getForObject(productUrl(favourite.getProductId()), ProductDto.class))
                .thenReturn(ProductDto.builder().productId(favourite.getProductId()).productTitle("Item").build());

        List<FavouriteDto> result = this.favouriteService.findAll();

        assertThat(result).hasSize(1);
        FavouriteDto dto = result.get(0);
        assertThat(dto.getUserId()).isEqualTo(favourite.getUserId());
        assertThat(dto.getProductId()).isEqualTo(favourite.getProductId());
        assertThat(dto.getLikeDate()).isEqualTo(favourite.getLikeDate());
        assertThat(dto.getUserDto().getUserId()).isEqualTo(favourite.getUserId());
        assertThat(dto.getProductDto().getProductId()).isEqualTo(favourite.getProductId());

        verify(this.restTemplate).getForObject(userUrl(favourite.getUserId()), UserDto.class);
        verify(this.restTemplate).getForObject(productUrl(favourite.getProductId()), ProductDto.class);
    }

    @Test
    void findAllShouldSkipDetailsWhenToggleIsDisabled() {
        Favourite favourite = buildFavourite(11, 22, LIKE_DATE);
        when(this.favouriteRepository.findAll()).thenReturn(List.of(favourite));
        when(this.featureToggleService.isFetchDetailsEnabled()).thenReturn(false);

        List<FavouriteDto> result = this.favouriteService.findAll();

        assertThat(result).hasSize(1);
        FavouriteDto dto = result.get(0);
        assertThat(dto.getUserId()).isEqualTo(favourite.getUserId());
        assertThat(dto.getUserDto()).isNotNull();
        assertThat(dto.getUserDto().getUserId()).isEqualTo(favourite.getUserId());
        assertThat(dto.getUserDto().getFirstName()).isNull();

        assertThat(dto.getProductDto()).isNotNull();
        assertThat(dto.getProductDto().getProductId()).isEqualTo(favourite.getProductId());
        assertThat(dto.getProductDto().getProductTitle()).isNull();

        verify(this.favouriteRepository).findAll();
        verifyNoInteractions(this.restTemplate);
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        Favourite favourite = buildFavourite(33, 44, LIKE_DATE);
        FavouriteId favouriteId = buildFavouriteId(favourite);
        when(this.favouriteRepository.findById(favouriteId)).thenReturn(Optional.of(favourite));
        when(this.restTemplate.getForObject(userUrl(favourite.getUserId()), UserDto.class))
                .thenReturn(UserDto.builder().userId(favourite.getUserId()).build());
        when(this.restTemplate.getForObject(productUrl(favourite.getProductId()), ProductDto.class))
                .thenReturn(ProductDto.builder().productId(favourite.getProductId()).build());

        FavouriteDto result = this.favouriteService.findById(favouriteId);

        assertThat(result.getUserId()).isEqualTo(favouriteId.getUserId());
        assertThat(result.getProductId()).isEqualTo(favouriteId.getProductId());
        assertThat(result.getLikeDate()).isEqualTo(favouriteId.getLikeDate());
        verify(this.favouriteRepository).findById(favouriteId);
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        FavouriteId favouriteId = new FavouriteId(99, 100, LIKE_DATE);
        when(this.favouriteRepository.findById(favouriteId)).thenReturn(Optional.empty());

        assertThrows(FavouriteNotFoundException.class, () -> this.favouriteService.findById(favouriteId));
        verify(this.favouriteRepository).findById(favouriteId);
    }

    @Test
    void saveShouldPersistMappedFavourite() {
        FavouriteDto payload = buildFavouriteDto(55, 66, LIKE_DATE);
        Favourite persisted = buildFavourite(payload.getUserId(), payload.getProductId(), payload.getLikeDate());
        when(this.favouriteRepository.save(any(Favourite.class))).thenReturn(persisted);

        FavouriteDto result = this.favouriteService.save(payload);

        ArgumentCaptor<Favourite> captor = ArgumentCaptor.forClass(Favourite.class);
        verify(this.favouriteRepository).save(captor.capture());
        Favourite saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(payload.getUserId());
        assertThat(saved.getProductId()).isEqualTo(payload.getProductId());

        assertThat(result.getUserId()).isEqualTo(payload.getUserId());
        assertThat(result.getProductId()).isEqualTo(payload.getProductId());
    }

    @Test
    void updateShouldPersistMappedFavourite() {
        FavouriteDto payload = buildFavouriteDto(77, 88, LIKE_DATE);
        when(this.favouriteRepository.save(any(Favourite.class)))
                .thenReturn(buildFavourite(payload.getUserId(), payload.getProductId(), payload.getLikeDate()));

        FavouriteDto result = this.favouriteService.update(payload);

        ArgumentCaptor<Favourite> captor = ArgumentCaptor.forClass(Favourite.class);
        verify(this.favouriteRepository).save(captor.capture());
        Favourite saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(payload.getUserId());
        assertThat(saved.getProductId()).isEqualTo(payload.getProductId());
        assertThat(result.getLikeDate()).isEqualTo(payload.getLikeDate());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        FavouriteId favouriteId = new FavouriteId(1, 2, LIKE_DATE);

        this.favouriteService.deleteById(favouriteId);

        verify(this.favouriteRepository).deleteById(favouriteId);
        verifyNoInteractions(this.restTemplate);
    }

    private static Favourite buildFavourite(int userId, int productId, LocalDateTime likeDate) {
        return Favourite.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(likeDate)
                .build();
    }

    private static FavouriteDto buildFavouriteDto(int userId, int productId, LocalDateTime likeDate) {
        return FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(likeDate)
                .build();
    }

    private static FavouriteId buildFavouriteId(Favourite favourite) {
        return new FavouriteId(favourite.getUserId(), favourite.getProductId(), favourite.getLikeDate());
    }

    private static String userUrl(int userId) {
        return AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
    }

    private static String productUrl(int productId) {
        return AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
    }
}
