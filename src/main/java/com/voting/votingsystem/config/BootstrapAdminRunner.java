package com.voting.votingsystem.config;

import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.repository.UserRepository;
import com.voting.votingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.fullName:Admin}")
    private String fullName;

    @Value("${app.bootstrap.admin.regNo:ADMIN-001}")
    private String regNo;

    @Value("${app.bootstrap.admin.email:admin@university.local}")
    private String email;

    @Value("${app.bootstrap.admin.password:admin123}")
    private String password;

    public BootstrapAdminRunner(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Bootstrap admin is disabled (app.bootstrap.admin.enabled=false).");
            return;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Bootstrap admin skipped: user already exists with email={}", email);
            return;
        }

        User admin = new User();
        admin.setFullName(fullName);
        admin.setRegNo(regNo);
        admin.setEmail(email);
        admin.setPassword(password); // encoded in UserService
        admin.setRole("ADMIN");
        userService.saveUser(admin);
        log.info("Bootstrap admin created: email={}, regNo={}", email, regNo);
    }
}
