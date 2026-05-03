package com.voting.votingsystem.service;

import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        validateNewUser(user);
        user.setRegNo(normalizeRegNo(user.getRegNo()));
        user.setEmail(normalizeEmail(user.getEmail()));
        user.setRole(normalizeRole(user.getRole()));

        // Provide user-friendly feedback instead of raw DB constraint errors.
        if (user.getRegNo() != null && userRepository.findByRegNo(user.getRegNo()).isPresent()) {
            throw new RuntimeException("User already exists with this registration number.");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank() && userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists with this email.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Race-condition safety: if two requests register at the same time, DB may still reject.
            if (user.getRegNo() != null && userRepository.findByRegNo(user.getRegNo()).isPresent()) {
                throw new RuntimeException("User already exists with this registration number.");
            }
            if (user.getEmail() != null && !user.getEmail().isBlank() && userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("User already exists with this email.");
            }
            throw new RuntimeException("User already exists.", ex);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByRegNo(String regNo) {
        return userRepository.findByRegNo(regNo);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public long countByRole(String role) {
        String normalized = normalizeRole(role);
        if (normalized == null || normalized.isEmpty()) {
            return 0L;
        }
        return userRepository.countByRole(normalized);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            validateUpdateUser(updatedUser);
            user.setFullName(updatedUser.getFullName());
            user.setRegNo(normalizeRegNo(updatedUser.getRegNo()));
            user.setEmail(normalizeEmail(updatedUser.getEmail()));
            user.setRole(normalizeRole(updatedUser.getRole()));

            // Friendly validation for unique constraints.
            if (user.getRegNo() != null) {
                Optional<User> existing = userRepository.findByRegNo(user.getRegNo());
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    throw new RuntimeException("User already exists with this registration number.");
                }
            }
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                Optional<User> existing = userRepository.findByEmail(user.getEmail());
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    throw new RuntimeException("User already exists with this email.");
                }
            }

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            try {
                return userRepository.save(user);
            } catch (DataIntegrityViolationException ex) {
                if (user.getRegNo() != null) {
                    Optional<User> existing = userRepository.findByRegNo(user.getRegNo());
                    if (existing.isPresent() && !existing.get().getId().equals(id)) {
                        throw new RuntimeException("User already exists with this registration number.");
                    }
                }
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    Optional<User> existing = userRepository.findByEmail(user.getEmail());
                    if (existing.isPresent() && !existing.get().getId().equals(id)) {
                        throw new RuntimeException("User already exists with this email.");
                    }
                }
                throw new RuntimeException("User already exists.", ex);
            }
        }).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? trimmed : trimmed.toLowerCase();
    }

    private static String normalizeRegNo(String regNo) {
        if (regNo == null) {
            return null;
        }
        return regNo.trim();
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String trimmed = role.trim().toUpperCase();
        // If someone stored ROLE_ADMIN by mistake, normalize to ADMIN.
        if (trimmed.startsWith("ROLE_")) {
            trimmed = trimmed.substring("ROLE_".length());
        }
        return trimmed;
    }

    private static void validateNewUser(User user) {
        if (user == null) {
            throw new RuntimeException("User is required.");
        }
        if (isBlank(user.getFullName())) {
            throw new RuntimeException("Full name is required.");
        }
        if (isBlank(user.getRegNo())) {
            throw new RuntimeException("Registration number is required.");
        }
        if (isBlank(user.getEmail())) {
            throw new RuntimeException("Email is required.");
        }
        if (!looksLikeEmail(user.getEmail())) {
            throw new RuntimeException("Please enter a valid email address.");
        }
        if (isBlank(user.getPassword())) {
            throw new RuntimeException("Password is required.");
        }
        if (user.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters.");
        }
        if (isBlank(user.getRole())) {
            throw new RuntimeException("Role is required.");
        }
    }

    private static void validateUpdateUser(User user) {
        if (user == null) {
            throw new RuntimeException("User is required.");
        }
        if (isBlank(user.getFullName())) {
            throw new RuntimeException("Full name is required.");
        }
        if (isBlank(user.getRegNo())) {
            throw new RuntimeException("Registration number is required.");
        }
        if (isBlank(user.getEmail())) {
            throw new RuntimeException("Email is required.");
        }
        if (!looksLikeEmail(user.getEmail())) {
            throw new RuntimeException("Please enter a valid email address.");
        }
        if (isBlank(user.getRole())) {
            throw new RuntimeException("Role is required.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean looksLikeEmail(String s) {
        // Beginner-friendly "good enough" validation for a university demo.
        String trimmed = s == null ? "" : s.trim();
        int at = trimmed.indexOf('@');
        int dot = trimmed.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < trimmed.length() - 1;
    }
}
