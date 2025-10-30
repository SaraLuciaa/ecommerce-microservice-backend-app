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

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        this.userService = new UserServiceImpl(this.userRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        User user = buildUser(1, "Jane", "Doe", "jane@example.com", "5554443333");
        when(this.userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> result = this.userService.findAll();

        assertThat(result).hasSize(1);
        UserDto dto = result.get(0);
        assertThat(dto.getUserId()).isEqualTo(user.getUserId());
        assertThat(dto.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(dto.getCredentialDto().getUsername()).isEqualTo(user.getCredential().getUsername());

        verify(this.userRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        User user = buildUser(2, "Sara", "Connor", "sara@example.com", "1234567890");
        when(this.userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        UserDto result = this.userService.findById(user.getUserId());

        assertThat(result.getUserId()).isEqualTo(user.getUserId());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.userRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> this.userService.findById(404));
        verify(this.userRepository).findById(404);
    }

    @Test
    void saveShouldPersistMappedEntity() {
        UserDto payload = buildUserDto(null, "New", "User", "new@example.com", "0001112222");
        User persisted = buildUser(5, payload.getFirstName(), payload.getLastName(), payload.getEmail(), payload.getPhone());
        when(this.userRepository.save(any(User.class))).thenReturn(persisted);

        UserDto result = this.userService.save(payload);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getEmail()).isEqualTo(payload.getEmail());
        assertThat(saved.getCredential().getUsername()).isEqualTo(payload.getCredentialDto().getUsername());

        assertThat(result.getUserId()).isEqualTo(persisted.getUserId());
    }

    @Test
    void updateShouldPersistMappedEntity() {
        UserDto payload = buildUserDto(7, "Updated", "Name", "updated@example.com", "9998887777");
        User persisted = buildUser(payload.getUserId(), payload.getFirstName(), payload.getLastName(), payload.getEmail(), payload.getPhone());
        when(this.userRepository.save(any(User.class))).thenReturn(persisted);

        UserDto result = this.userService.update(payload);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(payload.getUserId());
        assertThat(saved.getEmail()).isEqualTo(payload.getEmail());

        assertThat(result.getFirstName()).isEqualTo(payload.getFirstName());
    }

    @Test
    void updateWithIdShouldApplyNewValues() {
        User existing = buildUser(12, "Legacy", "User", "legacy@example.com", "1112220000");
        when(this.userRepository.findById(existing.getUserId())).thenReturn(Optional.of(existing));
        UserDto payload = buildUserDto(null, "Modern", "User", "modern@example.com", "3334445555");
        User persisted = buildUser(existing.getUserId(), payload.getFirstName(), payload.getLastName(), payload.getEmail(), payload.getPhone());
        when(this.userRepository.save(any(User.class))).thenReturn(persisted);

        UserDto result = this.userService.update(existing.getUserId(), payload);

        verify(this.userRepository).findById(existing.getUserId());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(existing.getUserId());
        assertThat(saved.getFirstName()).isEqualTo(payload.getFirstName());
        assertThat(saved.getEmail()).isEqualTo(payload.getEmail());

        assertThat(result.getUserId()).isEqualTo(existing.getUserId());
        assertThat(result.getEmail()).isEqualTo(payload.getEmail());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.userService.deleteById(55);

        verify(this.userRepository).deleteById(55);
    }

    @Test
    void findByUsernameShouldReturnDtoWhenFound() {
    User user = buildUser(25, "Lookup", "User", "lookup@example.com", "1122334455");
    when(this.userRepository.findByCredentialUsername("lookup"))
        .thenReturn(Optional.of(user));

        UserDto result = this.userService.findByUsername("lookup");

        assertThat(result.getUserId()).isEqualTo(user.getUserId());
        assertThat(result.getCredentialDto().getUsername()).isEqualTo(user.getCredential().getUsername());
    }

    @Test
    void findByUsernameShouldThrowWhenMissing() {
    when(this.userRepository.findByCredentialUsername("missing"))
        .thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> this.userService.findByUsername("missing"));
        verify(this.userRepository).findByCredentialUsername("missing");
    }

    private static User buildUser(Integer userId, String firstName, String lastName, String email, String phone) {
        return User.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .imageUrl("image.jpg")
                .email(email)
                .phone(phone)
                .credential(Credential.builder()
                        .credentialId(100)
                        .username(firstName.toLowerCase())
                        .password("secret")
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .user(null)
                        .build())
                .build();
    }

    private static UserDto buildUserDto(Integer userId, String firstName, String lastName, String email, String phone) {
        return UserDto.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .imageUrl("image.jpg")
                .email(email)
                .phone(phone)
                .credentialDto(CredentialDto.builder()
                        .credentialId(100)
                        .username(firstName.toLowerCase())
                        .password("secret")
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();
    }
}
