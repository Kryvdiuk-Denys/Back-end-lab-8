package com.coworking.booking.messaging;

import com.coworking.common.dto.WorkspaceCreatedEvent;
import com.coworking.common.messaging.RabbitTopology;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkspaceCreatedEventListener {

    private final WorkspaceCreatedEventService eventService;

    public WorkspaceCreatedEventListener(WorkspaceCreatedEventService eventService) {
        this.eventService = eventService;
    }

    @RabbitListener(queues = RabbitTopology.WORKSPACE_CREATED_QUEUE)
    public void handle(WorkspaceCreatedEvent event) {
        boolean recorded = eventService.process(event);
        if (recorded) {
            log.info("Recorded workspace-created event {} for workspace {}", event.eventId(), event.workspaceId());
        } else {
            log.info("Skipped duplicate workspace-created event {}", event.eventId());
        }
    }
}
