package com.coworking.booking.controller;

import com.coworking.booking.WorkspaceClient;
import com.coworking.booking.WorkspaceUsageGuard;
import com.coworking.common.dto.WorkspaceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceRestProxyController {

    private final WorkspaceClient workspaceClient;
    private final WorkspaceUsageGuard workspaceUsageGuard;

    public WorkspaceRestProxyController(WorkspaceClient workspaceClient, WorkspaceUsageGuard workspaceUsageGuard) {
        this.workspaceClient = workspaceClient;
        this.workspaceUsageGuard = workspaceUsageGuard;
    }

    @GetMapping
    public List<WorkspaceDto> findAll() {
        return workspaceClient.findAll();
    }

    @GetMapping("/{id}")
    public WorkspaceDto findById(@PathVariable Long id) {
        return workspaceClient.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceDto create(@Valid @RequestBody WorkspaceDto dto) {
        return workspaceClient.create(dto);
    }

    @PutMapping("/{id}")
    public WorkspaceDto update(@PathVariable Long id, @Valid @RequestBody WorkspaceDto dto) {
        return workspaceClient.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workspaceUsageGuard.ensureWorkspaceCanBeDeleted(id);
        workspaceClient.delete(id);
    }
}
