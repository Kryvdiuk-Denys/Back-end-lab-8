package com.coworking.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Message contract shared by the workspace producer and booking consumer. */
public record WorkspaceCreatedEvent(
        UUID eventId,
        Long workspaceId,
        String workspaceCode,
        String workspaceName,
        String workspaceType,
        Integer capacity,
        BigDecimal hourlyRate,
        boolean active,
        LocalDateTime occurredAt
) {
}
