package com.voting.votingsystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String role = request.getParameter("loginRole");
        boolean admin = role != null && role.trim().equalsIgnoreCase("ADMIN");
        String targetUrl = admin ? "/admin-login?error=true" : "/student-login?error=true";
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
}

