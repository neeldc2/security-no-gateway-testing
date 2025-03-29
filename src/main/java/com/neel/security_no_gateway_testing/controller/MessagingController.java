package com.neel.security_no_gateway_testing.controller;

import com.neel.security_no_gateway_testing.messaging.ActiveMqProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.ActiveMqConstants.EMAIL_QUEUE;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.KafkaConstants.NOTIFICATION_EMAIL_TOPIC;

@RequiredArgsConstructor
@RestController
@RequestMapping("/messaging")
public class MessagingController {

    //private final KafkaProducer kafkaProducer;
    private final ActiveMqProducer activeMqProducer;

    private Integer kafkaKey = 0;

    /*@PostMapping("/kafka/send")
    public String sendKafkaMessage(@RequestParam String message) {
        kafkaKey++;
        kafkaProducer.sendMessage(NOTIFICATION_EMAIL_TOPIC, message, kafkaKey.toString());
        return "Message sent to Kafka topic: " + NOTIFICATION_EMAIL_TOPIC;
    }*/

    @PostMapping("/activemq/send")
    public String sendActiveMqMessage(@RequestParam String message) {
        activeMqProducer.sendMessage(EMAIL_QUEUE, message);
        return "Message sent to ActiveMq queue: " + NOTIFICATION_EMAIL_TOPIC;
    }
}
