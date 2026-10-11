package com.gmt.rental.config;

import com.gmt.rental.entity.User;
import com.gmt.rental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrapInitializer(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${gmt.admin.email:}") String email,
            @Value("${gmt.admin.password:}") String password
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        var existing = users.findByEmail(email.trim());
        if (existing.isPresent()) {
            User currentAdmin = existing.get();
            if (Boolean.FALSE.equals(currentAdmin.getEmailVerified()) || currentAdmin.getEmailVerified() == null) {
                currentAdmin.setEmailVerified(true);
                users.save(currentAdmin);
            }
            return;
        }
        User admin = new User();
        admin.setEmail(email.trim());
        admin.setNom("Administrateur");
        admin.setRole("ADMIN");
        admin.setActif(true);
        admin.setEmailVerified(true);
        admin.setMotDePasse(passwordEncoder.encode(password));
        users.save(admin);
    }
}
