package com.cdp.header6;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ReceiveLogHeader1 {
    private static final String EXCHANGE_NAME = "header_test";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.HEADERS);

        // The API requires a routing key, but in fact if you are using a header exchange the
        // value of the routing key is not used in the routing. You can receive information
        // from the sender here as the routing key is still available in the received message.
        String routingKeyFromUser = "ourTestRoutingKey";


        // The map for the headers.
        Map<String, Object> headers = new HashMap<>();
        // 消费端需要设置x-match属性，有两个值可选any和all，不设置默认为all
        // any为header键值对中一对完全匹配就可路由
        // all为header键值对中所有完全匹配就可路由
        // 完全匹配指的是key和value均要相等，不能只有key相等，value也要相等
        headers.put("x-match", "any");
        headers.put("header1", "value1");
        headers.put("header2", "value2");


        String queueName = channel.queueDeclare("Header队列any模式", true, false, false, null).getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, routingKeyFromUser, headers);

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }
}