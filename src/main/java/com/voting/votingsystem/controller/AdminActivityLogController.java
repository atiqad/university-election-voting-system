package com.voting.votingsystem.controller;

import com.voting.votingsystem.entity.ActivityLog;
import com.voting.votingsystem.service.ActivityLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/activity-logs")
public class AdminActivityLogController {

    private final ActivityLogService activityLogService;

    public AdminActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public String activityLogs(Model model) {
        List<ActivityLog> logs = activityLogService.getRecentLogs();
        model.addAttribute("logs", logs);
        return "admin-activity-logs";
    }
}

