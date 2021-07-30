package com.cdp.topic5;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class ReceiveLogsTopic2 {

    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare("测试topic类型队列2,测试#", true, false, false, null).getQueue();

        /*

        * (star) can substitute for exactly one word.
            正好可以代替一个词。
        # (hash) can substitute for zero or more words.
            可以代替零个或多个单词。

        topic.*：可以匹配topic.AAA，topic.BBB
        topic.#：可以匹配topic，topic.AAA，topic.AAA.BBB

         */
        channel.queueBind(queueName, EXCHANGE_NAME, "topic.#");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }
}