package com.neel.security_no_gateway_testing.messaging;

import org.springframework.stereotype.Component;

@Component
public class ActiveMqConsumer {

    //@JmsListener(destination = NOTIFICATION_EMAIL_QUEUE)
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
    }

}
