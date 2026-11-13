package com.coworking.booking.messaging;

import com.coworking.booking.dto.WorkspaceCreatedEventDto;
import com.coworking.booking.repository.WorkspaceEventStore;
import com.coworking.common.dto.WorkspaceCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceCreatedEventService {

    private final WorkspaceEventStore eventStore;

    public WorkspaceCreatedEventService(WorkspaceEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Transactional
    public boolean process(WorkspaceCreatedEvent event) {
        return eventStore.recordIfAbsent(event);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceCreatedEventDto> findAll() {
        return eventStore.findAll();
    }
}
