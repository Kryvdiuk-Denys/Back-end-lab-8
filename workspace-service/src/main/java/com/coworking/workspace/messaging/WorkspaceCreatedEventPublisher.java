package com.coworking.workspace.messaging;

import com.coworking.common.dto.WorkspaceCreatedEvent;
import com.coworking.common.messaging.RabbitTopology;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkspaceCreatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public WorkspaceCreatedEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(WorkspaceCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitTopology.WORKSPACE_EVENTS_EXCHANGE,
                RabbitTopology.WORKSPACE_CREATED_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setMessageId(event.eventId().toString());
                    return message;
                }
        );
    }
}
