package com.selimhorri.app.e2e;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.selimhorri.app.business.user.model.RoleBasedAuthority;

@Configuration
public class TestStubsConfig {
    @Bean
    @Primary
    UserDetailsService testUserDetailsService(final PasswordEncoder encoder) {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(final String username) {
                return User.withUsername("test")
                    .password(encoder.encode("password"))
                    .roles(RoleBasedAuthority.ROLE_USER.getRole().replace("ROLE_", ""))
                    .build();
            }
        };
    }
}


