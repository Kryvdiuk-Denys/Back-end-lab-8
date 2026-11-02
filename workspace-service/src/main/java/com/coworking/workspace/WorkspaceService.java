package com.coworking.workspace;

import com.coworking.common.dto.WorkspaceDto;
import com.coworking.workspace.entity.Workspace;
import com.coworking.workspace.mapper.WorkspaceMapper;
import com.coworking.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMapper workspaceMapper;

    public WorkspaceService(WorkspaceRepository workspaceRepository, WorkspaceMapper workspaceMapper) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMapper = workspaceMapper;
    }

    public List<WorkspaceDto> findAll() {
        return workspaceMapper.toDtoList(workspaceRepository.findAllByOrderByNameAsc());
    }

    public WorkspaceDto findById(Long id) {
        return workspaceMapper.toDto(getEntity(id));
    }

    @Transactional
    public WorkspaceDto create(WorkspaceDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Workspace data must not be null");
        }
        Workspace workspace = workspaceMapper.toEntity(dto);
        Workspace created = workspaceRepository.save(workspace);
        if (created.getCode() == null || created.getCode().isBlank()) {
            created.setCode("WS-" + created.getId());
            created = workspaceRepository.save(created);
        }
        return workspaceMapper.toDto(created);
    }

    @Transactional
    public WorkspaceDto update(Long id, WorkspaceDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Workspace data must not be null");
        }
        Workspace workspace = getEntity(id);
        workspaceMapper.updateEntityFromDto(dto, workspace);
        return workspaceMapper.toDto(workspace);
    }

    @Transactional
    public void delete(Long id) {
        workspaceRepository.delete(getEntity(id));
    }

    public Workspace getEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace " + id + " not found"));
    }
}
