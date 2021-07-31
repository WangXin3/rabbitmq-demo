package com.cdp.confirms7;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BooleanSupplier;

public class PublisherConfirms {

    static final int MESSAGE_COUNT = 50_000;

    static Connection createConnection() throws Exception {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setUsername("guest");
        cf.setPassword("guest");
        return cf.newConnection();
    }

    public static void main(String[] args) throws Exception {
        publishMessagesIndividually();
        publishMessagesInBatch();
        handlePublishConfirmsAsynchronously();
    }

    /**
     * 这种确认方式有一个最大的缺点就是:发布速度特别的慢， 因为如果没有确认发布的消息就会
     * 阻塞所有后续消息的发布，这种方式最多提供每秒不超过数百条发布消息的吞吐量。当然对于某
     * 些应用程序来说这可能已经足够了。
     *
     * @throws Exception
     */
    static void publishMessagesIndividually() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            // 开启发布确认
            ch.confirmSelect();
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                ch.basicPublish("", queue, null, body.getBytes());
                // waitForConfirmsOrDie只有在消息被确认的时候返回，如果设置了超时时间，也会在超时时间到达之后返回
                ch.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages individually in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    /**
     * 上面那种方式非常慢，与单个等待确认消息相比，先发布一批消息然后一起确认可以极大地
     * 提高吞吐量，当然这种方式的缺点就是:当发生故障导致发布出现问题时， 不知道是哪个消息出现
     * 问题了， 我们必须将整个批处理保存在内存中，以记录重要的信息而后重新发布消息。当然这种
     * 方案仍然是同步的，也一样阻塞消息的发布。
     *
     * @throws Exception
     */
    static void publishMessagesInBatch() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();

            int batchSize = 100;
            int outstandingMessageCount = 0;

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                ch.basicPublish("", queue, null, body.getBytes());
                outstandingMessageCount++;

                if (outstandingMessageCount == batchSize) {
                    // 等待确认
                    ch.waitForConfirmsOrDie(5_000);
                    outstandingMessageCount = 0;
                }
            }

            if (outstandingMessageCount > 0) {
                ch.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("Published %,d messages in batch in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    /**
     * 异步确认虽然编程逻辑比上两个要复杂，但是性价比最高，无论是可靠性还是效率都没得说，
     * 他是利用回调函数来达到消息可靠性传递的。
     *
     * @throws Exception /
     */
    static void handlePublishConfirmsAsynchronously() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);

            ch.confirmSelect();

            ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();


            ConfirmCallback cleanOutstandingConfirms = new ConfirmCallback() {
                /**
                 * 确认收到消息的一个回调
                 * 1.消息序列号
                 * 2.true 可以确认小于等于当前序列号的消息
                 *   false 确认当前序列号消息
                 * @param deliveryTag /
                 * @param multiple /
                 * @throws IOException /
                 */
                @Override
                public void handle(long deliveryTag, boolean multiple) throws IOException {
                    if (multiple) {
                        //返回的是小于等于当前序列号的未确认消息 是一个 map
                        ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(
                                deliveryTag, true
                        );
                        // 清除该部分未确认消息
                        confirmed.clear();
                    } else {
                        // 只清除当前序列号的消息
                        outstandingConfirms.remove(deliveryTag);
                    }
                }
            };

            ConfirmCallback nackCallback = new ConfirmCallback() {
                @Override
                public void handle(long deliveryTag, boolean multiple) throws IOException {
                    String body = outstandingConfirms.get(deliveryTag);
                    System.err.format(
                            "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                            body, deliveryTag, multiple
                    );
                    cleanOutstandingConfirms.handle(deliveryTag, multiple);
                }
            };

            // ackCallback，nackCallback
            ch.addConfirmListener(cleanOutstandingConfirms, nackCallback);

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                outstandingConfirms.put(ch.getNextPublishSeqNo(), body);
                ch.basicPublish("", queue, null, body.getBytes());
            }

            if (!waitUntil(Duration.ofSeconds(60), () -> outstandingConfirms.isEmpty())) {
                throw new IllegalStateException("All messages could not be confirmed in 60 seconds");
            }

            long end = System.nanoTime();
            System.out.format("Published %,d messages and handled confirms asynchronously in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    static boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {
            Thread.sleep(100L);
            waited = +100;
        }
        return condition.getAsBoolean();
    }

}