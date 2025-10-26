package com.selimhorri.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:user_service_integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

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

        List<Credential> credentials = this.credentialRepository.findAll();
        assertThat(credentials).hasSize(1);

        this.persistedUserId = user.getUserId();
        this.persistedCredentialId = credentials.get(0).getCredentialId();
    }

    @Test
    void findAllShouldReturnPersistedUsers() {
        List<UserDto> users = this.userService.findAll();

        assertThat(users).hasSize(1);
        UserDto dto = users.get(0);
        assertThat(dto.getUserId()).isEqualTo(this.persistedUserId);
        assertThat(dto.getCredentialDto().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByIdShouldReturnUser() {
        UserDto dto = this.userService.findById(this.persistedUserId);

        assertThat(dto.getFirstName()).isEqualTo("Alice");
        assertThat(dto.getCredentialDto().getUsername()).isEqualTo("alice");
    }

    @Test
    void saveShouldPersistUser() {
        UserDto payload = buildUserDto(null, "Bob", "Builder", "bob@example.com", "3014412233", "bob", RoleBasedAuthority.ROLE_ADMIN);

        UserDto saved = this.userService.save(payload);

        assertThat(saved.getUserId()).isNotNull();
        User reloaded = this.userRepository.findById(saved.getUserId()).orElseThrow();
        assertThat(reloaded.getCredential().getUsername()).isEqualTo("bob");
    }

    @Test
    void updateShouldModifyExistingUser() {
        UserDto payload = buildUserDto(this.persistedUserId, "Alice", "Wonder", "alice.updated@example.com", "3001112233", "alice", RoleBasedAuthority.ROLE_USER);
        payload.getCredentialDto().setCredentialId(this.persistedCredentialId);

        UserDto updated = this.userService.update(payload);

        assertThat(updated.getEmail()).isEqualTo("alice.updated@example.com");
        User reloaded = this.userRepository.findById(this.persistedUserId).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("alice.updated@example.com");
    }

    @Test
    void updateByIdShouldOverrideValues() {
        UserDto payload = buildUserDto(null, "Alice", "Wonder", "alice.override@example.com", "3001112233", "alice", RoleBasedAuthority.ROLE_USER);
        payload.getCredentialDto().setCredentialId(this.persistedCredentialId);

        UserDto updated = this.userService.update(this.persistedUserId, payload);

        assertThat(updated.getEmail()).isEqualTo("alice.override@example.com");
        User reloaded = this.userRepository.findById(this.persistedUserId).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("alice.override@example.com");
    }

    @Test
    void deleteByIdShouldRemoveUser() {
        this.userService.deleteById(this.persistedUserId);

        assertThat(this.userRepository.findById(this.persistedUserId)).isEmpty();
        assertThat(this.credentialRepository.findAll()).isEmpty();
    }

    @Test
    void findByUsernameShouldReturnUser() {
        UserDto dto = this.userService.findByUsername("alice");

        assertThat(dto.getUserId()).isEqualTo(this.persistedUserId);
        assertThat(dto.getCredentialDto().getUsername()).isEqualTo("alice");
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
