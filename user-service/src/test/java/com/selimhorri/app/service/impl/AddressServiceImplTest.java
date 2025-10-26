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

import com.selimhorri.app.domain.Address;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.AddressNotFoundException;
import com.selimhorri.app.repository.AddressRepository;
import com.selimhorri.app.service.AddressService;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        this.addressService = new AddressServiceImpl(this.addressRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        Address address = buildAddress(1, 10, "123 Main St", "7600", "Cali");
        when(this.addressRepository.findAll()).thenReturn(List.of(address));

        List<AddressDto> result = this.addressService.findAll();

        assertThat(result).hasSize(1);
        AddressDto dto = result.get(0);
        assertThat(dto.getAddressId()).isEqualTo(address.getAddressId());
        assertThat(dto.getFullAddress()).isEqualTo(address.getFullAddress());
        assertThat(dto.getUserDto().getUserId()).isEqualTo(address.getUser().getUserId());

        verify(this.addressRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        Address address = buildAddress(2, 11, "456 North St", "1100", "Bogota");
        when(this.addressRepository.findById(address.getAddressId())).thenReturn(Optional.of(address));

        AddressDto result = this.addressService.findById(address.getAddressId());

        assertThat(result.getAddressId()).isEqualTo(address.getAddressId());
        assertThat(result.getCity()).isEqualTo(address.getCity());
        assertThat(result.getUserDto().getUserId()).isEqualTo(address.getUser().getUserId());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.addressRepository.findById(555)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> this.addressService.findById(555));
        verify(this.addressRepository).findById(555);
    }

    @Test
    void saveShouldPersistMappedEntity() {
        AddressDto payload = buildAddressDto(null, 22, "789 South St", "4400", "Medellin");
        Address persisted = buildAddress(3, 22, payload.getFullAddress(), payload.getPostalCode(), payload.getCity());
        when(this.addressRepository.save(any(Address.class))).thenReturn(persisted);

        AddressDto result = this.addressService.save(payload);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(this.addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertThat(saved.getAddressId()).isNull();
        assertThat(saved.getFullAddress()).isEqualTo(payload.getFullAddress());
        assertThat(saved.getUser().getUserId()).isEqualTo(payload.getUserDto().getUserId());

        assertThat(result.getAddressId()).isEqualTo(persisted.getAddressId());
        assertThat(result.getCity()).isEqualTo(payload.getCity());
    }

    @Test
    void updateShouldPersistMappedEntity() {
        AddressDto payload = buildAddressDto(7, 33, "101 Central Ave", "5500", "Barranquilla");
        Address persisted = buildAddress(payload.getAddressId(), 33, payload.getFullAddress(), payload.getPostalCode(), payload.getCity());
        when(this.addressRepository.save(any(Address.class))).thenReturn(persisted);

        AddressDto result = this.addressService.update(payload);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(this.addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertThat(saved.getAddressId()).isEqualTo(payload.getAddressId());
        assertThat(saved.getCity()).isEqualTo(payload.getCity());

        assertThat(result.getPostalCode()).isEqualTo(payload.getPostalCode());
    }

    @Test
    void updateWithIdShouldApplyNewValues() {
        Address existing = buildAddress(15, 99, "Old Address", "0000", "Old City");
        when(this.addressRepository.findById(existing.getAddressId())).thenReturn(Optional.of(existing));
        AddressDto payload = buildAddressDto(null, 99, "New Address", "9999", "New City");
        Address persisted = buildAddress(existing.getAddressId(), 99, payload.getFullAddress(), payload.getPostalCode(), payload.getCity());
        when(this.addressRepository.save(any(Address.class))).thenReturn(persisted);

        AddressDto result = this.addressService.update(existing.getAddressId(), payload);

        verify(this.addressRepository).findById(existing.getAddressId());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(this.addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertThat(saved.getAddressId()).isEqualTo(existing.getAddressId());
        assertThat(saved.getFullAddress()).isEqualTo(payload.getFullAddress());
        assertThat(saved.getCity()).isEqualTo(payload.getCity());

        assertThat(result.getAddressId()).isEqualTo(existing.getAddressId());
        assertThat(result.getFullAddress()).isEqualTo(payload.getFullAddress());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.addressService.deleteById(88);

        verify(this.addressRepository).deleteById(88);
    }

    private static Address buildAddress(Integer addressId, Integer userId, String fullAddress, String postalCode, String city) {
        return Address.builder()
                .addressId(addressId)
                .fullAddress(fullAddress)
                .postalCode(postalCode)
                .city(city)
                .user(buildUser(userId))
                .build();
    }

    private static AddressDto buildAddressDto(Integer addressId, Integer userId, String fullAddress, String postalCode, String city) {
        return AddressDto.builder()
                .addressId(addressId)
                .fullAddress(fullAddress)
                .postalCode(postalCode)
                .city(city)
                .userDto(buildUserDto(userId))
                .build();
    }

    private static User buildUser(Integer userId) {
        return User.builder()
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("image.jpg")
                .email("john.doe@example.com")
                .phone("1112223333")
                .build();
    }

    private static UserDto buildUserDto(Integer userId) {
        return UserDto.builder()
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("image.jpg")
                .email("john.doe@example.com")
                .phone("1112223333")
                .build();
    }
}
