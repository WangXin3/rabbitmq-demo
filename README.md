## MQ简介

MQ是Message Queue的简写，字面意思就是消息队列。

作用：

* 削峰填谷

  ![](https://img-blog.csdnimg.cn/20190429100227446.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2FsbGVuc2FuZHk=,size_16,color_FFFFFF,t_70)



* 应用解耦

  模块之间不直接调用，通过中间件来做消息流转，降低应用之间的强耦合。

* 异步处理

  消息发送方将消息发到消息队列，不同步等待消息处理结果，直接返回，提高消息发送方的处理效率，消息接收方收到消息后开始异步处理。



## RabbitMQ



## 概念介绍

生产者：产生数据并发送消息的是生产者  

消费者：消费与接收具有相似的含义。消费者大多时候是一个等待接收消息的程序。 请注意生产者，消费
者和消息中间件很多时候并不在同一机器上。同一个应用程序既可以是生产者又是可以是消费者  

队列：队列是 RabbitMQ 内部使用的一种数据结构， 尽管消息流经 RabbitMQ 和应用程序，但它们只能存
储在队列中。队列仅受主机的内存和磁盘限制的约束，本质上是一个大的消息缓冲区。许多生产者可
以将消息发送到一个队列，许多消费者可以尝试从一个队列接收数据。这就是我们使用队列的方式  

交换机：交换机是 RabbitMQ 非常重要的一个部件，一方面它接收来自生产者的消息，另一方面它将消息
推送到队列中。交换机必须知道如何处理它接收到的消息，是将这些消息推送到特定队列还是推
送到多个队列，亦或者是把消息丢弃，这个得有交换机类型决定  



## 交换机的四种类型

* direct 直连

  根据routingKey 路由到具体的队列

  ![](https://www.rabbitmq.com/img/tutorials/direct-exchange.png)

  ![](https://www.rabbitmq.com/img/tutorials/direct-exchange-multiple.png)

  p发送消息携带 交换机名，routingKey，就可以发送给指定队列

* fanout 广播

  ![](https://www.rabbitmq.com/img/tutorials/bindings.png)

  交换机的type为fanout，只要绑定到该类型的交换机，生产者发送的消息，绑定的所有队列都可以收到

* topic 主题

  ![](https://www.rabbitmq.com/img/tutorials/python-five.png)

  routingKey模糊匹配类型的交换机

  *：正好可以代替一个词。

  * AA.orange.BB
  * AA.BB.orange.BB
  * AA.BB.rabbit 

  #：可以代替零个或多个单词 

  * lazy
  * lazy.AA
  * lazy.BBB.CCC

* headers 头（key-value全匹配模式）：用的比较少



## 官网的七种模式

* Hello World
* Work queues
* Publish/Subscribe
* Routing
* Topics
* RPC
* Publisher Confirms





## ack(Acknowledge)介绍

应答机制，消费者默认开启的是自动ack，也就是自动应答，消费者收到消息就会自动ack，队列就会删除这条消息。一般我们使用需要使用切换为手动ack，也就是等我们消费端来用代码控制何时应答，或者拒绝消息。



## 发布确认简介

生产者将信道设置成 confirm 模式，一旦信道进入 confirm 模式， 所有在该信道上面发布的消息都将会被指派一个唯一的 ID(从 1 开始)，一旦消息被投递到所有匹配的队列之后，MQ就会发送一个确认给生产者(包含消息的唯一 ID)，这就使得生产者知道消息已经正确到达目的队列了，如果消息和队列是可持久化的，那么确认消息会在将消息写入磁盘之后发出， MQ 回传给生产者的确认消息中 delivery-tag 域包含了确认消息的序列号，此外 MQ 也可以设置basic.ack 的 multiple 域，表示到这个序列号之前的所有消息都已经得到了处理。  



