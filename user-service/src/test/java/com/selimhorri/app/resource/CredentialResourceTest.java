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
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.CredentialService;

@WebMvcTest(CredentialResource.class)
class CredentialResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CredentialService credentialService;

    private CredentialDto credentialDto;

    @BeforeEach
    void setUp() {
        this.credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("john")
                .password("secret")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .userDto(UserDto.builder()
                        .userId(10)
                        .firstName("John")
                        .lastName("Doe")
                        .imageUrl("img.jpg")
                        .email("john@example.com")
                        .phone("1112223333")
                        .build())
                .build();
    }

    @Test
    void findAllShouldReturnCollectionResponse() throws Exception {
        when(this.credentialService.findAll()).thenReturn(List.of(this.credentialDto));

        this.mockMvc.perform(get("/api/credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].username", equalTo(this.credentialDto.getUsername())));

        verify(this.credentialService).findAll();
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        when(this.credentialService.findById(1)).thenReturn(this.credentialDto);

        this.mockMvc.perform(get("/api/credentials/{credentialId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleBasedAuthority", equalTo(this.credentialDto.getRoleBasedAuthority().name())));

        verify(this.credentialService).findById(1);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.credentialService.save(any(CredentialDto.class))).thenReturn(this.credentialDto);

        this.mockMvc.perform(post("/api/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.credentialDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId", equalTo(this.credentialDto.getCredentialId())));

        ArgumentCaptor<CredentialDto> captor = ArgumentCaptor.forClass(CredentialDto.class);
        verify(this.credentialService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUsername()).isEqualTo(this.credentialDto.getUsername());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.credentialService.update(any(CredentialDto.class))).thenReturn(this.credentialDto);

        this.mockMvc.perform(put("/api/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.credentialDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(this.credentialDto.getUsername())));

        verify(this.credentialService).update(any(CredentialDto.class));
    }

    @Test
    void updateWithIdShouldPassParsedIdentifier() throws Exception {
    when(this.credentialService.update(eq(1), any(CredentialDto.class))).thenReturn(this.credentialDto);

        this.mockMvc.perform(put("/api/credentials/{credentialId}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.credentialDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(this.credentialDto.getUsername())));

    ArgumentCaptor<CredentialDto> captor = ArgumentCaptor.forClass(CredentialDto.class);
    verify(this.credentialService).update(eq(1), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getUsername()).isEqualTo(this.credentialDto.getUsername());
    }

    @Test
    void deleteShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/credentials/{credentialId}", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.credentialService).deleteById(7);
    }

    @Test
    void findByUsernameShouldReturnDto() throws Exception {
        when(this.credentialService.findByUsername("john")).thenReturn(this.credentialDto);

        this.mockMvc.perform(get("/api/credentials/username/{username}", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo(this.credentialDto.getUsername())));

        verify(this.credentialService).findByUsername("john");
    }
}
