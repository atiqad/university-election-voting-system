package com.voting.votingsystem.service;

import com.voting.votingsystem.entity.ActivityLog;
import com.voting.votingsystem.entity.User;
import com.voting.votingsystem.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Minimal activity logger used by services (create/update/delete operations).
 *
 * This implementation logs to the application log AND stores activity logs in DB.
 */
@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;
    private final Clock clock;

    public ActivityLogService(ActivityLogRepository activityLogRepository, Clock clock) {
        this.activityLogRepository = activityLogRepository;
        this.clock = clock;
    }

    public void log(String action, String details) {
        log(null, action, details);
    }

    public void log(User user, String action, String details) {
        // Keep the format stable so it is easy to grep in logs.
        log.info("[ACTIVITY] action={} details={}", safe(action), safe(details));

        ActivityLog entry = new ActivityLog();
        entry.setUser(user);
        entry.setAction(safe(action));
        entry.setDetails(safe(details));
        entry.setPerformedBy(resolvePerformedBy(user));
        entry.setCreatedAt(Instant.now(clock));
        activityLogRepository.save(entry);
    }

    public List<ActivityLog> getRecentLogs() {
        return activityLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private static String resolvePerformedBy(User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String name = safe(auth.getName());
            if (!name.isBlank()) {
                return name;
            }
        }

        // If a concrete User object was provided explicitly, prefer an identifier from it.
        if (user != null) {
            try {
                // In this project, login is email-based, so this is usually the most useful identifier.
                String email = safe(user.getEmail());
                if (!email.isBlank()) {
                    return email;
                }
            } catch (Exception ignored) {
                // Keep logging robust even if User fields change.
            }
        }

        return "ADMIN";
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim();
    }
}
