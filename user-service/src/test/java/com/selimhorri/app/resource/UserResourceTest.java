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
import com.selimhorri.app.service.UserService;

@WebMvcTest(UserResource.class)
class UserResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        this.userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("img.jpg")
                .email("john@example.com")
                .phone("1112223333")
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
        when(this.userService.findAll()).thenReturn(List.of(this.userDto));

        this.mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].userId", equalTo(this.userDto.getUserId())));

        verify(this.userService).findAll();
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        when(this.userService.findById(1)).thenReturn(this.userDto);

        this.mockMvc.perform(get("/api/users/{userId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", equalTo(this.userDto.getFirstName())));

        verify(this.userService).findById(1);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.userService.save(any(UserDto.class))).thenReturn(this.userDto);

        this.mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", equalTo(this.userDto.getUserId())));

        ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
        verify(this.userService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getEmail()).isEqualTo(this.userDto.getEmail());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.userService.update(any(UserDto.class))).thenReturn(this.userDto);

        this.mockMvc.perform(put("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", equalTo(this.userDto.getEmail())));

        verify(this.userService).update(any(UserDto.class));
    }

    @Test
    void updateWithIdShouldPassParsedIdentifier() throws Exception {
    when(this.userService.update(eq(1), any(UserDto.class))).thenReturn(this.userDto);

        this.mockMvc.perform(put("/api/users/{userId}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName", equalTo(this.userDto.getLastName())));

    ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
    verify(this.userService).update(eq(1), captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getLastName()).isEqualTo(this.userDto.getLastName());
    }

    @Test
    void deleteShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/users/{userId}", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.userService).deleteById(7);
    }

    @Test
    void findByUsernameShouldReturnDto() throws Exception {
        when(this.userService.findByUsername("john")).thenReturn(this.userDto);

        this.mockMvc.perform(get("/api/users/username/{username}", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential.username", equalTo(this.userDto.getCredentialDto().getUsername())));

        verify(this.userService).findByUsername("john");
    }
}
