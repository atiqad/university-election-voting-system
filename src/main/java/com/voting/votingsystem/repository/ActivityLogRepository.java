package com.voting.votingsystem.repository;

import com.voting.votingsystem.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop50ByOrderByCreatedAtDesc();
}
