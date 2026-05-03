package com.voting.votingsystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        String loginType = request.getParameter("loginType");
        boolean wantsAdmin = loginType != null && loginType.trim().equalsIgnoreCase("ADMIN");
        boolean wantsStudent = loginType != null && loginType.trim().equalsIgnoreCase("STUDENT");

        boolean mismatch = (wantsAdmin && !isAdmin) || (wantsStudent && isAdmin);
        if (mismatch) {
            // Prevent cross-role login from the wrong page (admin vs student) while keeping core auth intact.
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            String redirect = wantsAdmin ? "/admin-login?roleError=true" : "/student-login?roleError=true";
            redirectStrategy.sendRedirect(request, response, redirect);
            return;
        }

        String targetUrl = isAdmin ? "/admin-dashboard" : "/student-dashboard";
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
}
