package com.coworking.booking;

import com.coworking.common.dto.WorkspaceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class WorkspaceClient {

    private final RestClient restClient;

    public WorkspaceClient(
            RestClient.Builder builder,
            @Value("${services.workspace.base-url:http://localhost:8082}") String baseUrl,
            @Value("${services.workspace.service-token:local-workspace-token}") String serviceToken
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Token", serviceToken)
                .defaultHeader("X-Tenant-ID", "booking-service")
                .build();
    }

    public List<WorkspaceDto> findAll() {
        return restClient.get()
                .uri("/api/workspaces")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public WorkspaceDto findById(Long id) {
        return exchange(() -> restClient.get()
                .uri("/api/workspaces/{id}", id)
                .retrieve()
                .body(WorkspaceDto.class), id);
    }

    public WorkspaceDto create(WorkspaceDto workspace) {
        return restClient.post()
                .uri("/api/workspaces")
                .body(workspace)
                .retrieve()
                .body(WorkspaceDto.class);
    }

    public WorkspaceDto update(Long id, WorkspaceDto workspace) {
        return exchange(() -> restClient.put()
                .uri("/api/workspaces/{id}", id)
                .body(workspace)
                .retrieve()
                .body(WorkspaceDto.class), id);
    }

    public void delete(Long id) {
        exchange(() -> {
            restClient.method(HttpMethod.DELETE)
                    .uri("/api/workspaces/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        }, id);
    }

    private <T> T exchange(WorkspaceRequest<T> request, Long id) {
        try {
            return request.execute();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Workspace " + id + " not found");
            }
            throw new IllegalStateException(
                    "Workspace service returned HTTP " + exception.getStatusCode().value(), exception
            );
        }
    }

    @FunctionalInterface
    private interface WorkspaceRequest<T> {
        T execute();
    }
}
