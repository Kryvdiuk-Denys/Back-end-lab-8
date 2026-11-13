package com.coworking.booking.repository;

import com.coworking.booking.dto.WorkspaceCreatedEventDto;
import com.coworking.common.dto.WorkspaceCreatedEvent;

import java.util.List;

public interface WorkspaceEventStore {

    boolean recordIfAbsent(WorkspaceCreatedEvent event);

    List<WorkspaceCreatedEventDto> findAll();
}
