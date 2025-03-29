package com.neel.security_no_gateway_testing.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neel.security_no_gateway_testing.dto.messaging.AddTenantEmailPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class ActiveMqProducer {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendMessage(String queueName, String message) {
        jmsTemplate.convertAndSend(queueName, message);
    }

    public void sendMessage(String queueName, AddTenantEmailPayload addTenantEmailPayload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(addTenantEmailPayload);
            jmsTemplate.convertAndSend(queueName, jsonPayload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
