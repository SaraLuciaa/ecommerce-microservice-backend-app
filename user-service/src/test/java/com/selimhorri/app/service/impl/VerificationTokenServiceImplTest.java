package com.selimhorri.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.selimhorri.app.domain.VerificationToken;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.VerificationTokenDto;
import com.selimhorri.app.exception.wrapper.VerificationTokenNotFoundException;
import com.selimhorri.app.repository.VerificationTokenRepository;
import com.selimhorri.app.service.VerificationTokenService;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceImplTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    private VerificationTokenService verificationTokenService;

    @BeforeEach
    void setUp() {
        this.verificationTokenService = new VerificationTokenServiceImpl(this.verificationTokenRepository);
    }

    @Test
    void findAllShouldReturnMappedDtos() {
        VerificationToken token = buildVerificationToken(1, "abc", LocalDate.of(2025, 10, 26));
        when(this.verificationTokenRepository.findAll()).thenReturn(List.of(token));

        List<VerificationTokenDto> result = this.verificationTokenService.findAll();

        assertThat(result).hasSize(1);
        VerificationTokenDto dto = result.get(0);
        assertThat(dto.getVerificationTokenId()).isEqualTo(token.getVerificationTokenId());
        assertThat(dto.getToken()).isEqualTo(token.getToken());
        assertThat(dto.getCredentialDto().getCredentialId()).isEqualTo(token.getCredential().getCredentialId());

        verify(this.verificationTokenRepository).findAll();
    }

    @Test
    void findByIdShouldReturnDtoWhenFound() {
        VerificationToken token = buildVerificationToken(2, "xyz", LocalDate.now());
        when(this.verificationTokenRepository.findById(token.getVerificationTokenId())).thenReturn(Optional.of(token));

        VerificationTokenDto result = this.verificationTokenService.findById(token.getVerificationTokenId());

        assertThat(result.getVerificationTokenId()).isEqualTo(token.getVerificationTokenId());
        assertThat(result.getToken()).isEqualTo(token.getToken());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(this.verificationTokenRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(VerificationTokenNotFoundException.class, () -> this.verificationTokenService.findById(404));
        verify(this.verificationTokenRepository).findById(404);
    }

    @Test
    void saveShouldPersistMappedEntity() {
        VerificationTokenDto payload = buildVerificationTokenDto(null, "payload", LocalDate.of(2025, 12, 25));
        VerificationToken persisted = buildVerificationToken(5, payload.getToken(), payload.getExpireDate());
        when(this.verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(persisted);

        VerificationTokenDto result = this.verificationTokenService.save(payload);

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(this.verificationTokenRepository).save(captor.capture());
        VerificationToken saved = captor.getValue();
        assertThat(saved.getVerificationTokenId()).isNull();
        assertThat(saved.getToken()).isEqualTo(payload.getToken());
        assertThat(saved.getCredential().getCredentialId()).isEqualTo(payload.getCredentialDto().getCredentialId());

        assertThat(result.getVerificationTokenId()).isEqualTo(persisted.getVerificationTokenId());
    }

    @Test
    void updateShouldPersistMappedEntity() {
        VerificationTokenDto payload = buildVerificationTokenDto(6, "update", LocalDate.of(2026, 1, 1));
        VerificationToken persisted = buildVerificationToken(payload.getVerificationTokenId(), payload.getToken(), payload.getExpireDate());
        when(this.verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(persisted);

        VerificationTokenDto result = this.verificationTokenService.update(payload);

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(this.verificationTokenRepository).save(captor.capture());
        VerificationToken saved = captor.getValue();
        assertThat(saved.getVerificationTokenId()).isEqualTo(payload.getVerificationTokenId());
        assertThat(saved.getToken()).isEqualTo(payload.getToken());

        assertThat(result.getExpireDate()).isEqualTo(payload.getExpireDate());
    }

    @Test
    void updateWithIdShouldApplyNewValues() {
        VerificationToken existing = buildVerificationToken(9, "legacy", LocalDate.of(2024, 5, 20));
        when(this.verificationTokenRepository.findById(existing.getVerificationTokenId())).thenReturn(Optional.of(existing));
        VerificationTokenDto payload = buildVerificationTokenDto(null, "refreshed", LocalDate.of(2027, 3, 15));
        VerificationToken persisted = buildVerificationToken(existing.getVerificationTokenId(), payload.getToken(), payload.getExpireDate());
        when(this.verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(persisted);

        VerificationTokenDto result = this.verificationTokenService.update(existing.getVerificationTokenId(), payload);

        verify(this.verificationTokenRepository).findById(existing.getVerificationTokenId());
        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(this.verificationTokenRepository).save(captor.capture());
        VerificationToken saved = captor.getValue();
        assertThat(saved.getVerificationTokenId()).isEqualTo(existing.getVerificationTokenId());
        assertThat(saved.getToken()).isEqualTo(payload.getToken());
        assertThat(saved.getExpireDate()).isEqualTo(payload.getExpireDate());

        assertThat(result.getVerificationTokenId()).isEqualTo(existing.getVerificationTokenId());
        assertThat(result.getToken()).isEqualTo(payload.getToken());
    }

    @Test
    void deleteByIdShouldDelegateToRepository() {
        this.verificationTokenService.deleteById(77);

        verify(this.verificationTokenRepository).deleteById(77);
    }

    private static VerificationToken buildVerificationToken(Integer id, String token, LocalDate expireDate) {
        return VerificationToken.builder()
                .verificationTokenId(id)
                .token(token)
                .expireDate(expireDate)
                .credential(buildCredential(200))
                .build();
    }

    private static VerificationTokenDto buildVerificationTokenDto(Integer id, String token, LocalDate expireDate) {
        return VerificationTokenDto.builder()
                .verificationTokenId(id)
                .token(token)
                .expireDate(expireDate)
                .credentialDto(buildCredentialDto(200))
                .build();
    }

    private static Credential buildCredential(Integer credentialId) {
        return Credential.builder()
                .credentialId(credentialId)
                .username("user" + credentialId)
                .password("secret")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .user(null)
                .build();
    }

    private static CredentialDto buildCredentialDto(Integer credentialId) {
        return CredentialDto.builder()
                .credentialId(credentialId)
                .username("user" + credentialId)
                .password("secret")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }
}
