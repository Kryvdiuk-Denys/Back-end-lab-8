package com.coworking.workspace.controller;

import com.coworking.common.dto.WorkspaceDto;
import com.coworking.workspace.WorkspaceService;
import com.coworking.workspace.annotation.CreateEndpoint;
import com.coworking.workspace.annotation.TenantId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceRestController {

    private final WorkspaceService workspaceService;

    public WorkspaceRestController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<WorkspaceDto> findAll(@TenantId(required = false) String tenantId) {
        return workspaceService.findAll();
    }

    @GetMapping("/{id}")
    public WorkspaceDto findById(@PathVariable Long id) {
        return workspaceService.findById(id);
    }

    @CreateEndpoint
    public WorkspaceDto create(@Valid @RequestBody WorkspaceDto dto) {
        return workspaceService.create(dto);
    }

    @PutMapping("/{id}")
    public WorkspaceDto update(@PathVariable Long id, @Valid @RequestBody WorkspaceDto dto) {
        return workspaceService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workspaceService.delete(id);
    }
}
