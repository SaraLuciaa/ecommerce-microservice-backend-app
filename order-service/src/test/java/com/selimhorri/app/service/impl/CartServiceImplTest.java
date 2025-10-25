package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestTemplate restTemplate;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        this.cartService = new CartServiceImpl(this.cartRepository, this.restTemplate);
    }

    @Test
    void findAllShouldReturnCartDtosWithUsers() {
        Cart cartOne = buildCart(1, 101);
        Cart cartTwo = buildCart(2, 202);
        when(this.cartRepository.findAll()).thenReturn(List.of(cartOne, cartTwo));
        UserDto userOne = buildUser(101);
        UserDto userTwo = buildUser(202);
        when(this.restTemplate.getForObject(userUrl(101), UserDto.class)).thenReturn(userOne);
        when(this.restTemplate.getForObject(userUrl(202), UserDto.class)).thenReturn(userTwo);

        List<CartDto> result = this.cartService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCartId()).isEqualTo(cartOne.getCartId());
        assertThat(result.get(0).getUserDto()).isEqualTo(userOne);
        assertThat(result.get(1).getCartId()).isEqualTo(cartTwo.getCartId());
        assertThat(result.get(1).getUserDto()).isEqualTo(userTwo);

        verify(this.cartRepository).findAll();
        verify(this.restTemplate).getForObject(userUrl(101), UserDto.class);
        verify(this.restTemplate).getForObject(userUrl(202), UserDto.class);
    }

    @Test
    void findByIdShouldReturnCartDtoWithUser() {
        Cart cart = buildCart(9, 303);
        when(this.cartRepository.findById(cart.getCartId())).thenReturn(Optional.of(cart));
        UserDto user = buildUser(cart.getUserId());
        when(this.restTemplate.getForObject(userUrl(cart.getUserId()), UserDto.class)).thenReturn(user);

        CartDto result = this.cartService.findById(cart.getCartId());

        assertThat(result.getCartId()).isEqualTo(cart.getCartId());
        assertThat(result.getUserDto()).isEqualTo(user);
        verify(this.cartRepository).findById(cart.getCartId());
        verify(this.restTemplate).getForObject(userUrl(cart.getUserId()), UserDto.class);
    }

    @Test
    void findByIdShouldThrowWhenCartMissing() {
        when(this.cartRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> this.cartService.findById(404));
        verify(this.cartRepository).findById(404);
    }

    @Test
    void saveShouldPersistAndReturnCartDto() {
        CartDto payload = CartDto.builder().userId(808).build();
        Cart persisted = buildCart(12, payload.getUserId());
        when(this.cartRepository.save(any(Cart.class))).thenReturn(persisted);

        CartDto result = this.cartService.save(payload);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(this.cartRepository).save(captor.capture());
        Cart savedEntity = captor.getValue();
        assertThat(savedEntity.getCartId()).isNull();
        assertThat(savedEntity.getUserId()).isEqualTo(payload.getUserId());

        assertThat(result.getCartId()).isEqualTo(persisted.getCartId());
        assertThat(result.getUserId()).isEqualTo(payload.getUserId());
    }

    @Test
    void updateShouldPersistChangesForDtoWithIdentifier() {
        CartDto payload = CartDto.builder().cartId(31).userId(909).build();
        Cart persisted = buildCart(payload.getCartId(), payload.getUserId());
        when(this.cartRepository.save(any(Cart.class))).thenReturn(persisted);

        CartDto result = this.cartService.update(payload);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(this.cartRepository).save(captor.capture());
        Cart savedEntity = captor.getValue();
        assertThat(savedEntity.getCartId()).isEqualTo(payload.getCartId());
        assertThat(savedEntity.getUserId()).isEqualTo(payload.getUserId());

        assertThat(result.getCartId()).isEqualTo(payload.getCartId());
        assertThat(result.getUserId()).isEqualTo(payload.getUserId());
    }

    @Test
    void updateByIdShouldOverrideIdentifierAndPersistChanges() {
        int cartId = 77;
        CartDto payload = CartDto.builder().userId(1001).build();
        Cart existing = buildCart(cartId, 555);
        when(this.cartRepository.findById(cartId)).thenReturn(Optional.of(existing));
        Cart persisted = buildCart(cartId, payload.getUserId());
        when(this.cartRepository.save(any(Cart.class))).thenReturn(persisted);

        CartDto result = this.cartService.update(cartId, payload);

        verify(this.cartRepository).findById(cartId);
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(this.cartRepository).save(captor.capture());
        Cart savedEntity = captor.getValue();
        assertThat(savedEntity.getCartId()).isEqualTo(cartId);
        assertThat(savedEntity.getUserId()).isEqualTo(payload.getUserId());

        assertThat(result.getCartId()).isEqualTo(cartId);
        assertThat(result.getUserId()).isEqualTo(payload.getUserId());
    }

    @Test
    void updateByIdShouldThrowWhenCartMissing() {
        when(this.cartRepository.findById(123)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> this.cartService.update(123, CartDto.builder().userId(1).build()));
        verify(this.cartRepository).findById(123);
        verify(this.cartRepository, times(0)).save(any(Cart.class));
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.cartService.deleteById(9);

        verify(this.cartRepository).deleteById(9);
    verify(this.restTemplate, times(0)).getForObject(anyString(), eq(UserDto.class));
    }

    private static Cart buildCart(Integer cartId, Integer userId) {
        return Cart.builder()
                .cartId(cartId)
                .userId(userId)
                .orders(Collections.emptySet())
                .build();
    }

    private static UserDto buildUser(Integer userId) {
        return UserDto.builder()
                .userId(userId)
                .firstName("First" + userId)
                .lastName("Last" + userId)
                .build();
    }

    private static String userUrl(Integer userId) {
        return AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
    }
}
