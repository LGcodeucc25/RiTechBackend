package com.ritech.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ritech.backend.config.RitechProperties;

class DemoDataSeederTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private DemoDataSeeder seeder(String userPassword, String managerPassword) {
        RitechProperties properties = new RitechProperties(
                new RitechProperties.Jwt("x".repeat(32), Duration.ofHours(8)),
                new RitechProperties.Cors(List.of()),
                new RitechProperties.Seed(true, userPassword, managerPassword));
        return new DemoDataSeeder(roleRepository, userRepository, passwordEncoder, properties);
    }

    @Test
    @DisplayName("Seeder inserts missing roles and users with hashed passwords")
    void insertsMissingData() {
        RoleEntity role = new RoleEntity("USUARIO");
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty(), Optional.empty(),
                Optional.of(role), Optional.of(role));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        seeder("first-secret", "second-secret").run(null);

        verify(roleRepository, times(2)).save(any(RoleEntity.class));
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Seeder never inserts existing rows and skips users without a password")
    void skipsExistingAndEmpty() {
        RoleEntity role = new RoleEntity("GERENTE");
        when(roleRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(role));
        when(userRepository.findByEmailIgnoreCase("usuario@ritech.co")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("gerente@ritech.co")).thenReturn(Optional.empty());

        seeder("", " ").run(null);

        verify(roleRepository, never()).save(any(RoleEntity.class));
        verify(userRepository, never()).save(any(User.class));
    }
}
