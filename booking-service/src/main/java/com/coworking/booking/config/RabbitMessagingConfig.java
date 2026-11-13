package com.coworking.booking.config;

import com.coworking.common.messaging.RabbitTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

    @Bean
    DirectExchange workspaceEventsExchange() {
        return new DirectExchange(RabbitTopology.WORKSPACE_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue workspaceCreatedQueue() {
        return QueueBuilder.durable(RabbitTopology.WORKSPACE_CREATED_QUEUE).build();
    }

    @Bean
    Binding workspaceCreatedBinding(Queue workspaceCreatedQueue, DirectExchange workspaceEventsExchange) {
        return BindingBuilder.bind(workspaceCreatedQueue)
                .to(workspaceEventsExchange)
                .with(RabbitTopology.WORKSPACE_CREATED_ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.coworking.common.dto");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
