package com.coworking.common.messaging;

public final class RabbitTopology {

    public static final String WORKSPACE_EVENTS_EXCHANGE = "coworking.workspace.events";
    public static final String WORKSPACE_CREATED_QUEUE = "coworking.booking.workspace-created";
    public static final String WORKSPACE_CREATED_ROUTING_KEY = "workspace.created";

    private RabbitTopology() {
    }
}
