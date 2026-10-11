package com.gmt.rental.security;

import com.gmt.rental.entity.User;
import com.gmt.rental.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
        String role = user.getRole() == null ? "USER" : user.getRole().toUpperCase();
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getMotDePasse() == null ? "" : user.getMotDePasse())
                .authorities(authority)
                .disabled(Boolean.FALSE.equals(user.getActif()) || Boolean.FALSE.equals(user.getEmailVerified()))
                .build();
    }
}
