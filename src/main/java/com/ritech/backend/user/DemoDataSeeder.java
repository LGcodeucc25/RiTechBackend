package com.ritech.backend.user;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ritech.backend.config.RitechProperties;

/**
 * Inserts the roles and demo users that are missing. Never updates or deletes rows.
 */
@Component
@ConditionalOnProperty(prefix = "ritech.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RitechProperties properties;

    public DemoDataSeeder(RoleRepository roleRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, RitechProperties properties) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> inserted = new ArrayList<>();
        for (Role role : Role.values()) {
            if (roleRepository.findByNameIgnoreCase(role.databaseName()).isEmpty()) {
                roleRepository.save(new RoleEntity(role.databaseName()));
                inserted.add("role " + role.databaseName());
            }
        }
        seedUser("Camila Rojas", "usuario@ritech.co", Role.USER, properties.seed().userPassword(), inserted);
        seedUser("Andrés Molina", "gerente@ritech.co", Role.MANAGER, properties.seed().managerPassword(), inserted);
        log.info("Demo data seeding finished; inserted: {}", inserted.isEmpty() ? "nothing" : String.join(", ", inserted));
    }

    private void seedUser(String name, String email, Role role, String password, List<String> inserted) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("Skipping demo user {} because its password variable is empty", email);
            return;
        }
        RoleEntity roleEntity = roleRepository.findByNameIgnoreCase(role.databaseName()).orElseThrow();
        userRepository.save(new User(name, email, passwordEncoder.encode(password), roleEntity));
        inserted.add("user " + email);
    }
}
