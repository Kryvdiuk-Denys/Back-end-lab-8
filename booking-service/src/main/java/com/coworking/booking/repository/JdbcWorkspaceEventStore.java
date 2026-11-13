package com.coworking.booking.repository;

import com.coworking.booking.dto.WorkspaceCreatedEventDto;
import com.coworking.common.dto.WorkspaceCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcWorkspaceEventStore implements WorkspaceEventStore {

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO workspace_created_event (
                event_id, workspace_id, workspace_code, workspace_name, workspace_type,
                capacity, hourly_rate, active, occurred_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String SELECT_ALL = """
            SELECT event_id, workspace_id, workspace_code, workspace_name, workspace_type,
                   capacity, hourly_rate, active, occurred_at
            FROM workspace_created_event
            ORDER BY occurred_at DESC, event_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcWorkspaceEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean recordIfAbsent(WorkspaceCreatedEvent event) {
        return jdbcTemplate.update(
                INSERT_IF_ABSENT,
                event.eventId(),
                event.workspaceId(),
                event.workspaceCode(),
                event.workspaceName(),
                event.workspaceType(),
                event.capacity(),
                event.hourlyRate(),
                event.active(),
                event.occurredAt()
        ) == 1;
    }

    @Override
    public List<WorkspaceCreatedEventDto> findAll() {
        return jdbcTemplate.query(SELECT_ALL, (resultSet, rowNumber) -> new WorkspaceCreatedEventDto(
                resultSet.getObject("event_id", java.util.UUID.class),
                resultSet.getLong("workspace_id"),
                resultSet.getString("workspace_code"),
                resultSet.getString("workspace_name"),
                resultSet.getString("workspace_type"),
                resultSet.getInt("capacity"),
                resultSet.getBigDecimal("hourly_rate"),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("occurred_at").toLocalDateTime()
        ));
    }
}
