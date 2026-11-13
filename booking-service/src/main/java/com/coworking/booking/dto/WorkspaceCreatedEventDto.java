package com.coworking.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceCreatedEventDto(
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
