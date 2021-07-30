package com.cdp.header6;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EmitLogHeader {

    private static final String EXCHANGE_NAME = "header_test";

    public static void main(String[] argv) throws Exception {

        // API 需要路由键，但实际上如果您使用标头交换，则路由中不会使用路由键的值。
        // 您可以在此处存储接收方的信息，因为路由密钥在接收到的消息中仍然可用。
        String routingKey = "ourTestRoutingKey";


        // 标题的映射。
        Map<String, Object> headers = new HashMap<>(16);
        headers.put("header1", "value1");
//        headers.put("header2", "value2");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.HEADERS);

            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();

            // MessageProperties.PERSISTENT_TEXT_PLAIN 是 AMQP.BasicProperties 的静态实例，它包含一个传递模式和一个优先级。 所以我们将它们传递给构建器。
            builder.deliveryMode(MessageProperties.PERSISTENT_TEXT_PLAIN.getDeliveryMode());
            builder.priority(MessageProperties.PERSISTENT_TEXT_PLAIN.getPriority());

            // 将标题添加到构建器。
            builder.headers(headers);

            // 使用构建器创建 BasicProperties 对象。
            AMQP.BasicProperties theProps = builder.build();

            String message = "Hello World!";

            // 现在我们添加标题。 此示例仅使用字符串标头，但它们也可以是整数
            channel.basicPublish(EXCHANGE_NAME, routingKey, theProps, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent message: '" + message + "'");
        }
    }
}