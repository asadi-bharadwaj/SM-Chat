package com.SM.ChatService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * **Summary:** Configuration class for Redis integration within the Chat Service.
 * 
 * **Flow:** Sets up the infrastructure required for Redis operations, including a {@link RedisTemplate} 
 * for data operations and a {@link RedisMessageListenerContainer} for handling Pub/Sub messages on the "chat-channel". 
 * It ensures all data is serialized and deserialized as JSON with proper Java 8 time support.
 * 
 * **Features:** Redis infrastructure setup, JSON serialization configuration, and Pub/Sub listener registration.
 */
@Configuration
public class RedisConfig {

    /**
     * **Summary:** Configures the primary Redis template for the application.
     * 
     * **Flow:** Sets up a {@link RedisTemplate} with {@link StringRedisSerializer} for keys and 
     * {@link Jackson2JsonRedisSerializer} for values. It registers {@link JavaTimeModule} with the ObjectMapper 
     * to handle {@link java.time.LocalDateTime} fields in entities.
     * 
     * **Features:** Standardized JSON serialization for Redis data storage.
     * 
     * @param connectionFactory The factory used to create Redis connections.
     * @return A configured {@link RedisTemplate} instance.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        return template;
    }

    /**
     * **Summary:** Configures the Redis message listener container.
     * 
     * **Flow:** Creates a container that manages the connection to Redis for Pub/Sub and registers the 
     * {@link MessageListenerAdapter} to listen on the "chat-channel" topic.
     * 
     * **Features:** Centralized Pub/Sub management and channel subscription.
     * 
     * @param connectionFactory The factory used to create Redis connections.
     * @param listenerAdapter The adapter that bridges Redis messages to the subscriber service.
     * @return A configured {@link RedisMessageListenerContainer}.
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("chat-channel"));
        return container;
    }

    /**
     * **Summary:** Configures the adapter that redirects Redis messages to the subscriber.
     * 
     * **Flow:** Maps the "onMessage" method of {@link RedisMessageSubscriber} to be the callback for Redis messages. 
     * It also configures JSON serialization for the incoming message payload.
     * 
     * **Features:** Message callback mapping and deserialization.
     * 
     * @param subscriber The service that will process the messages.
     * @return A configured {@link MessageListenerAdapter}.
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        adapter.setSerializer(serializer);
        return adapter;
    }
}
