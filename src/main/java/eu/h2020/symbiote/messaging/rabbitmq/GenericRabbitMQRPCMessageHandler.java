package eu.h2020.symbiote.messaging.rabbitmq;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;


/**
 * This class is to communicate with RabbitMQ. Initially created by Elena
 *
 * @author: Elena Garrido
 * @version: 18/01/2017

 */
public class GenericRabbitMQRPCMessageHandler <T,O> {

    private static Log logger = LogFactory.getLog( GenericRabbitMQRPCMessageHandler.class );

    @Value("${symbiote.rabbitmq.host.ip}")
    String rabbitMQHostIP;

    private Connection connection;
    private Channel channel;
    private QueueingConsumer consumer;
    String replyQueueName;
    String requestQueueName;
    @SuppressWarnings("rawtypes")
	Class clazz;
    String exchangeName;

    public GenericRabbitMQRPCMessageHandler(String exchangeName, String requestQueueName, String replyQueueName, @SuppressWarnings("rawtypes") Class clazz) {
    	logger.info("Creating with requestQueueName:"+requestQueueName+" and replyQueueName:" +replyQueueName);
    	this.replyQueueName = replyQueueName;
    	this.requestQueueName = requestQueueName;
    	this.exchangeName = exchangeName;
    	this.clazz = clazz;
    }
    
    public void connect() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQHostIP);
        connection = factory.newConnection();
        channel = connection.createChannel();
        replyQueueName = channel.queueDeclare().getQueue();
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);    	
    }

    /**
     * Method for sending a message to specified 'queue' on RabbitMQ server. Object is converted to Json.
     *
     * @param queueName
     * @param object
     * @throws Exception
     */

     @SuppressWarnings("unchecked")
	public O sendMessage(T object) throws Exception {
    	 O result = null;
         String corrId = java.util.UUID.randomUUID().toString();
         Gson gson = new Gson();
         String objectInJson = gson.toJson(object);
         
         BasicProperties props = new BasicProperties
                 .Builder()
                 .correlationId(corrId)
                 .replyTo(replyQueueName)
                 .build();
         
         channel.basicPublish(exchangeName, requestQueueName, props, objectInJson.getBytes());

         while (true) {
             QueueingConsumer.Delivery delivery = consumer.nextDelivery();
             if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                 String response = new String(delivery.getBody());
                 result = (O)gson.fromJson(response, clazz);
                 logger.info("Result "+result);
                 break;
             }
         }

         return result;

     }

     public void close() throws Exception {
         connection.close();
     }
    
}
