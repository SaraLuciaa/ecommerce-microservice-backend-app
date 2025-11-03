package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.VerificationTokenDto;
import com.selimhorri.app.service.VerificationTokenService;

@WebMvcTest(VerificationTokenResource.class)
class VerificationTokenResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerificationTokenService verificationTokenService;

    private VerificationTokenDto verificationTokenDto;

    @BeforeEach
    void setUp() {
        this.verificationTokenDto = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("token")
                .expireDate(LocalDate.of(2025, 10, 26))
                .credentialDto(CredentialDto.builder()
                        .credentialId(10)
                        .username("john")
                        .password("secret")
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();
    }

    @Test
    void findAllShouldReturnCollectionResponse() throws Exception {
        when(this.verificationTokenService.findAll()).thenReturn(List.of(this.verificationTokenDto));

        this.mockMvc.perform(get("/api/verificationTokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].token", equalTo(this.verificationTokenDto.getToken())));

        verify(this.verificationTokenService).findAll();
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        when(this.verificationTokenService.findById(1)).thenReturn(this.verificationTokenDto);

        this.mockMvc.perform(get("/api/verificationTokens/{verificationTokenId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", equalTo(this.verificationTokenDto.getToken())));

        verify(this.verificationTokenService).findById(1);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.verificationTokenService.save(any(VerificationTokenDto.class))).thenReturn(this.verificationTokenDto);

        this.mockMvc.perform(post("/api/verificationTokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.verificationTokenDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId", equalTo(this.verificationTokenDto.getVerificationTokenId())));

        ArgumentCaptor<VerificationTokenDto> captor = ArgumentCaptor.forClass(VerificationTokenDto.class);
        verify(this.verificationTokenService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getToken()).isEqualTo(this.verificationTokenDto.getToken());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.verificationTokenService.update(any(VerificationTokenDto.class))).thenReturn(this.verificationTokenDto);

        this.mockMvc.perform(put("/api/verificationTokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.verificationTokenDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", equalTo(this.verificationTokenDto.getToken())));

        verify(this.verificationTokenService).update(any(VerificationTokenDto.class));
    }

    @Test
    void updateWithIdShouldPassParsedIdentifier() throws Exception {
    when(this.verificationTokenService.update(eq(1), any(VerificationTokenDto.class))).thenReturn(this.verificationTokenDto);

        this.mockMvc.perform(put("/api/verificationTokens/{verificationTokenId}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.verificationTokenDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", equalTo(this.verificationTokenDto.getToken())));

    ArgumentCaptor<VerificationTokenDto> captor = ArgumentCaptor.forClass(VerificationTokenDto.class);
    verify(this.verificationTokenService).update(eq(1), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getToken()).isEqualTo(this.verificationTokenDto.getToken());
    }

    @Test
    void deleteShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/verificationTokens/{verificationTokenId}", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.verificationTokenService).deleteById(7);
    }
}
