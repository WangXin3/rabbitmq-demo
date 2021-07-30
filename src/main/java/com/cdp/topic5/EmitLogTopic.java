package com.cdp.topic5;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class EmitLogTopic {

    private static final String EXCHANGE_NAME = "topic_logs";

    private static final String[] routingKeys = {"topic.AAA", "topic.AAA.BBB", "topic", "topic.AAA.BBB.CCC", "topic.BBB"};

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            for (int i = 0; i < 20; i++) {
                String routingKey = routingKeys[i % routingKeys.length];
                String message = routingKey + "消息" + i;

                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
            }
        }
    }

}