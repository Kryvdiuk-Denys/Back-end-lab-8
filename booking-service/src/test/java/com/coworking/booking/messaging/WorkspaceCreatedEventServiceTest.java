package com.coworking.booking.messaging;

import com.coworking.booking.repository.WorkspaceEventStore;
import com.coworking.common.dto.WorkspaceCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceCreatedEventServiceTest {

    @Mock
    private WorkspaceEventStore eventStore;

    @InjectMocks
    private WorkspaceCreatedEventService eventService;

    @Test
    void processingTheSameEventTwiceRecordsOnlyTheFirstDelivery() {
        WorkspaceCreatedEvent event = new WorkspaceCreatedEvent(
                UUID.randomUUID(), 7L, "WS-007", "Quiet Room", "ROOM", 4,
                new BigDecimal("35.00"), true, LocalDateTime.parse("2026-11-04T10:00:00")
        );
        when(eventStore.recordIfAbsent(event)).thenReturn(true, false);

        assertTrue(eventService.process(event));
        assertFalse(eventService.process(event));

        verify(eventStore, times(2)).recordIfAbsent(event);
    }
}
