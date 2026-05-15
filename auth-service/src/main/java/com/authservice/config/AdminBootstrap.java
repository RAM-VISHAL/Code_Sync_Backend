package com.authservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;

@Configuration
public class AdminBootstrap {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            java.util.Optional<User> adminOpt = userRepository.findByEmail("admin@codesync.com");
            if (adminOpt.isEmpty()) {
                User admin = new User();
                admin.setUsername("superadmin");
                admin.setEmail("admin@codesync.com");
                admin.setPasswordHash(passwordEncoder.encode("SecureAdmin@123")); 
                admin.setRole(Role.ADMIN);
                admin.setProvider("LOCAL");
                
                userRepository.save(admin);
                System.out.println("✅ Super Admin Account Created Successfully!");
            } else {
                User existingAdmin = adminOpt.get();
                if (existingAdmin.getRole() != Role.ADMIN) {
                    existingAdmin.setRole(Role.ADMIN);
                    userRepository.save(existingAdmin);
                    System.out.println("✅ Existing Super Admin Account Role Updated to ADMIN!");
                }
            }
        };
    }
}
