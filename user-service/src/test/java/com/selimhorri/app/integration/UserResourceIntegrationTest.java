package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:user_resource_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.config.import=optional:file:./",
        "SPRING_CONFIG_IMPORT=optional:file:./",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "server.servlet.context-path="
})
@AutoConfigureMockMvc
class UserResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    private Integer persistedUserId;
    private Integer persistedCredentialId;

    @BeforeEach
    void setUp() {
        this.credentialRepository.deleteAll();
        this.userRepository.deleteAll();

        User user = buildUser("Alice", "Wonder", "alice@example.com", "3001112233", "alice", RoleBasedAuthority.ROLE_USER);
        user = this.userRepository.save(user);
        this.userRepository.flush();

        this.persistedUserId = user.getUserId();
        this.persistedCredentialId = this.credentialRepository.findAll()
                .stream()
                .findFirst()
                .map(Credential::getCredentialId)
                .orElseThrow();
    }

    @Test
    void findAllShouldReturnCollection() throws Exception {
        this.mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].userId").value(this.persistedUserId))
                .andExpect(jsonPath("$.collection[0].credential.username").value("alice"));
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        this.mockMvc.perform(get("/api/users/{userId}", this.persistedUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(this.persistedUserId))
                .andExpect(jsonPath("$.credential.username").value("alice"));
    }

    @Test
    void saveShouldPersistUser() throws Exception {
        UserDto payload = buildUserDto(null, "Bob", "Builder", "bob@example.com", "3014412233", "bob", RoleBasedAuthority.ROLE_ADMIN);

        this.mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.credential.username").value("bob"));

        assertThat(this.userRepository.findByCredentialUsername("bob")).isPresent();
    }

    @Test
    void updateShouldModifyUser() throws Exception {
        UserDto payload = buildUserDto(this.persistedUserId, "Alice", "Wonder", "alice.updated@example.com", "3001112233", "alice", RoleBasedAuthority.ROLE_USER);
        payload.getCredentialDto().setCredentialId(this.persistedCredentialId);

        this.mockMvc.perform(put("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice.updated@example.com"));

        User reloaded = this.userRepository.findById(this.persistedUserId).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("alice.updated@example.com");
    }

    @Test
    void updateByIdShouldOverrideValues() throws Exception {
        UserDto payload = buildUserDto(null, "Alice", "Wonder", "alice.override@example.com", "3001112233", "alice", RoleBasedAuthority.ROLE_USER);
        payload.getCredentialDto().setCredentialId(this.persistedCredentialId);

        this.mockMvc.perform(put("/api/users/{userId}", this.persistedUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice.override@example.com"));

        User reloaded = this.userRepository.findById(this.persistedUserId).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("alice.override@example.com");
    }

    @Test
    void deleteShouldRemoveUser() throws Exception {
        this.mockMvc.perform(delete("/api/users/{userId}", this.persistedUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertThat(this.userRepository.findById(this.persistedUserId)).isEmpty();
        assertThat(this.credentialRepository.findAll()).isEmpty();
    }

    @Test
    void findByUsernameShouldReturnDto() throws Exception {
        this.mockMvc.perform(get("/api/users/username/{username}", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(this.persistedUserId))
                .andExpect(jsonPath("$.credential.username").value("alice"));
    }

    private static User buildUser(String firstName, String lastName, String email, String phone, String username, RoleBasedAuthority authority) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .imageUrl("image.png")
                .email(email)
                .phone(phone)
                .build();

        Credential credential = Credential.builder()
                .username(username)
                .password("password")
                .roleBasedAuthority(authority)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .user(user)
                .build();

        user.setCredential(credential);
        return user;
    }

    private static UserDto buildUserDto(Integer userId, String firstName, String lastName, String email, String phone, String username, RoleBasedAuthority authority) {
        return UserDto.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .imageUrl("image.png")
                .email(email)
                .phone(phone)
                .credentialDto(CredentialDto.builder()
                        .credentialId(null)
                        .username(username)
                        .password("password")
                        .roleBasedAuthority(authority)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();
    }
}
