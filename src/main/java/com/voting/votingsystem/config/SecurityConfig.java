package com.voting.votingsystem.config;

import com.voting.votingsystem.security.RoleBasedAuthenticationSuccessHandler;
import com.voting.votingsystem.security.RoleBasedAuthenticationFailureHandler;
import com.voting.votingsystem.security.RoleBasedLogoutSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RoleBasedAuthenticationSuccessHandler successHandler,
                                                   RoleBasedAuthenticationFailureHandler failureHandler,
                                                   RoleBasedLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                // Keep CSRF enabled for browser forms, but ignore typical JSON API endpoints
                .csrf(csrf -> csrf.ignoringRequestMatchers("/users/**", "/votes/**", "/elections/**", "/candidates/**"))
                .authorizeHttpRequests(auth -> auth
                        // Static assets (CSS, JS, images, favicon, etc.)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/css/**", "/style.css", "/images/**").permitAll()

                        // Public pages
                        .requestMatchers("/login", "/login/", "/student-login", "/admin-login", "/register", "/register/", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/register", "/register/").permitAll()

                        // Page access (role-based)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin-dashboard").hasRole("ADMIN")
                        .requestMatchers("/student-dashboard").hasRole("STUDENT")
                        .requestMatchers("/vote-page").hasRole("STUDENT")
                        .requestMatchers("/student-results-page").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/submit-vote").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/candidates/new").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/candidates/edit/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/candidates/update/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/candidates/delete/**").hasRole("ADMIN")

                        // Shared authenticated pages (both ADMIN and STUDENT can view)
                        .requestMatchers("/elections-page", "/candidates-page", "/results-page")
                        .hasAnyRole("ADMIN", "STUDENT")

                        // Admin-only REST endpoints for managing elections/candidates
                        .requestMatchers(HttpMethod.POST, "/elections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/elections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/elections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/candidates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/candidates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/candidates/**").hasRole("ADMIN")

                        // Votes
                        .requestMatchers(HttpMethod.POST, "/votes/cast").hasRole("STUDENT")
                        .requestMatchers("/votes/results/**").hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers("/votes/**").hasRole("ADMIN")

                        // Users API should be admin-only (registration is handled via /register page)
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/student-login")
                        // Spring Security will handle POST /login automatically
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
