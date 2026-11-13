package com.coworking.booking.controller;

import com.coworking.booking.dto.WorkspaceCreatedEventDto;
import com.coworking.booking.messaging.WorkspaceCreatedEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/workspaces")
public class WorkspaceEventController {

    private final WorkspaceCreatedEventService eventService;

    public WorkspaceEventController(WorkspaceCreatedEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<WorkspaceCreatedEventDto> findAll() {
        return eventService.findAll();
    }
}
