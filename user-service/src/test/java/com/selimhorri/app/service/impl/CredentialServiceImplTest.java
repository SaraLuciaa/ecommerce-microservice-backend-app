package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.domain.VerificationToken;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.VerificationTokenDto;
import com.selimhorri.app.exception.wrapper.CredentialNotFoundException;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.service.CredentialService;

@ExtendWith(MockitoExtension.class)
class CredentialServiceImplTest {

    @Mock
    private CredentialRepository credentialRepository;

    private CredentialService credentialService;

    @BeforeEach
    void setUp() {
        this.credentialService = new CredentialServiceImpl(this.credentialRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        Credential credential = buildCredential(1, "john", RoleBasedAuthority.ROLE_USER, true);
        when(this.credentialRepository.findAll()).thenReturn(List.of(credential));

        List<CredentialDto> result = this.credentialService.findAll();

        assertThat(result).hasSize(1);
        CredentialDto dto = result.get(0);
        assertThat(dto.getCredentialId()).isEqualTo(credential.getCredentialId());
        assertThat(dto.getUsername()).isEqualTo(credential.getUsername());
        assertThat(dto.getUserDto().getUserId()).isEqualTo(credential.getUser().getUserId());

        verify(this.credentialRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        Credential credential = buildCredential(2, "anna", RoleBasedAuthority.ROLE_ADMIN, false);
        when(this.credentialRepository.findById(credential.getCredentialId())).thenReturn(Optional.of(credential));

        CredentialDto result = this.credentialService.findById(credential.getCredentialId());

        assertThat(result.getCredentialId()).isEqualTo(credential.getCredentialId());
        assertThat(result.getRoleBasedAuthority()).isEqualTo(credential.getRoleBasedAuthority());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.credentialRepository.findById(888)).thenReturn(Optional.empty());

        assertThrows(CredentialNotFoundException.class, () -> this.credentialService.findById(888));
        verify(this.credentialRepository).findById(888);
    }

    @Test
    void saveShouldPersistMappedEntity() {
        CredentialDto payload = buildCredentialDto(null, "new-user", RoleBasedAuthority.ROLE_USER, true);
        Credential persisted = buildCredential(4, payload.getUsername(), payload.getRoleBasedAuthority(), payload.getIsEnabled());
        when(this.credentialRepository.save(any(Credential.class))).thenReturn(persisted);

        CredentialDto result = this.credentialService.save(payload);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(this.credentialRepository).save(captor.capture());
        Credential saved = captor.getValue();
        assertThat(saved.getCredentialId()).isNull();
        assertThat(saved.getUsername()).isEqualTo(payload.getUsername());
        assertThat(saved.getUser().getUserId()).isEqualTo(payload.getUserDto().getUserId());

        assertThat(result.getCredentialId()).isEqualTo(persisted.getCredentialId());
    }

    @Test
    void updateShouldPersistMappedEntity() {
        CredentialDto payload = buildCredentialDto(6, "existing", RoleBasedAuthority.ROLE_USER, false);
        Credential persisted = buildCredential(payload.getCredentialId(), payload.getUsername(), payload.getRoleBasedAuthority(), payload.getIsEnabled());
        when(this.credentialRepository.save(any(Credential.class))).thenReturn(persisted);

        CredentialDto result = this.credentialService.update(payload);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(this.credentialRepository).save(captor.capture());
        Credential saved = captor.getValue();
        assertThat(saved.getCredentialId()).isEqualTo(payload.getCredentialId());
        assertThat(saved.getRoleBasedAuthority()).isEqualTo(payload.getRoleBasedAuthority());

        assertThat(result.getUsername()).isEqualTo(payload.getUsername());
    }

    @Test
    void updateWithIdShouldApplyNewValues() {
        Credential existing = buildCredential(9, "legacy", RoleBasedAuthority.ROLE_USER, true);
        when(this.credentialRepository.findById(existing.getCredentialId())).thenReturn(Optional.of(existing));
        CredentialDto payload = buildCredentialDto(null, "updated", RoleBasedAuthority.ROLE_ADMIN, false);
        Credential persisted = buildCredential(existing.getCredentialId(), payload.getUsername(), payload.getRoleBasedAuthority(), payload.getIsEnabled());
        when(this.credentialRepository.save(any(Credential.class))).thenReturn(persisted);

        CredentialDto result = this.credentialService.update(existing.getCredentialId(), payload);

        verify(this.credentialRepository).findById(existing.getCredentialId());
        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(this.credentialRepository).save(captor.capture());
        Credential saved = captor.getValue();
        assertThat(saved.getCredentialId()).isEqualTo(existing.getCredentialId());
        assertThat(saved.getUsername()).isEqualTo(payload.getUsername());
        assertThat(saved.getRoleBasedAuthority()).isEqualTo(payload.getRoleBasedAuthority());

        assertThat(result.getCredentialId()).isEqualTo(existing.getCredentialId());
        assertThat(result.getUsername()).isEqualTo(payload.getUsername());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.credentialService.deleteById(44);

        verify(this.credentialRepository).deleteById(44);
    }

    @Test
    void findByUsernameShouldReturnDtoWhenFound() {
        Credential credential = buildCredential(12, "lookup", RoleBasedAuthority.ROLE_USER, true);
        when(this.credentialRepository.findByUsername("lookup")).thenReturn(Optional.of(credential));

        CredentialDto result = this.credentialService.findByUsername("lookup");

        assertThat(result.getCredentialId()).isEqualTo(credential.getCredentialId());
        assertThat(result.getUsername()).isEqualTo(credential.getUsername());
    }

    @Test
    void findByUsernameShouldThrowWhenMissing() {
        when(this.credentialRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> this.credentialService.findByUsername("missing"));
        verify(this.credentialRepository).findByUsername("missing");
    }

    private static Credential buildCredential(Integer credentialId, String username, RoleBasedAuthority role, boolean enabled) {
        return Credential.builder()
                .credentialId(credentialId)
                .username(username)
                .password("secret")
                .roleBasedAuthority(role)
                .isEnabled(enabled)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .user(buildUser(20))
                .verificationTokens(Set.of(buildVerificationToken(100)))
                .build();
    }

    private static CredentialDto buildCredentialDto(Integer credentialId, String username, RoleBasedAuthority role, boolean enabled) {
        return CredentialDto.builder()
                .credentialId(credentialId)
                .username(username)
                .password("secret")
                .roleBasedAuthority(role)
                .isEnabled(enabled)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .userDto(buildUserDto(20))
                .verificationTokenDtos(Set.of(buildVerificationTokenDto(100)))
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

    private static VerificationToken buildVerificationToken(Integer id) {
        return VerificationToken.builder()
                .verificationTokenId(id)
                .token("token")
                .build();
    }

    private static VerificationTokenDto buildVerificationTokenDto(Integer id) {
        return VerificationTokenDto.builder()
                .verificationTokenId(id)
                .token("token")
                .build();
    }
}
