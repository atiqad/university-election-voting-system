package com.voting.votingsystem.security;

import com.voting.votingsystem.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = normalizeEmail(email);

        com.voting.votingsystem.entity.User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByEmail(normalized))
                .or(() -> userRepository.findByEmail(email == null ? null : email.trim()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String role = user.getRole() == null ? "STUDENT" : user.getRole().trim();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // already BCrypt-encoded when saved
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? trimmed : trimmed.toLowerCase();
    }
}
