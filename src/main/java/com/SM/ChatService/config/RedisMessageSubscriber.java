package com.SM.ChatService.config;

import com.SM.ChatService.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * **Summary:** Service that handles messages received from Redis Pub/Sub.
 * 
 * **Flow:** Listens for messages on the configured Redis channel ("chat-channel"), deserializes the JSON payload, 
 * and routes it to local WebSocket (STOMP) subscribers using {@link SimpMessagingTemplate}. This enables 
 * real-time communication across multiple instances of the Chat Service.
 * 
 * **Features:** Cross-instance message broadcasting, real-time chat delivery, typing indicator synchronization, 
 * call signal routing, and read receipt updates.
 */
@Service
@Slf4j
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * **Summary:** Constructor for RedisMessageSubscriber.
     * 
     * **Flow:** Injects the messaging template for WebSocket broadcasting and an object mapper for JSON deserialization.
     * 
     * **Features:** Dependency injection and initialization.
     * 
     * @param messagingTemplate The template used to send messages to local WebSocket clients.
     * @param objectMapper The mapper used to convert generic message data into Java entities.
     */
    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * **Summary:** Entry point for all messages received from the Redis "chat-channel".
     * 
     * **Flow:** 
     * 1. Receives a message and the channel name.
     * 2. Deserializes the message payload into a Map.
     * 3. Determines the message "type" (e.g., CHAT_MESSAGE, TYPING_EVENT).
     * 4. Converts the "data" portion of the payload into the appropriate entity if necessary.
     * 5. Broadcasts the message to specific STOMP topics (e.g., /topic/chat/{threadId}, /topic/inbox/{userId}).
     * 
     * **Features:** Dynamic message routing, multi-topic broadcasting, and cross-instance event synchronization.
     * 
     * @param message The raw message payload received from Redis (usually a Map after adapter processing).
     * @param channel The name of the Redis channel the message was received on.
     */
    public void onMessage(Object message, String channel) {
        log.info("REDIS-SUB: Received message on channel {}", channel);
        try {
            Map<String, Object> payload = (Map<String, Object>) message;
            String type = (String) payload.get("type");
            Object data = payload.get("data");

            if ("CHAT_MESSAGE".equals(type)) {
                ChatMessage chatMessage = objectMapper.convertValue(data, ChatMessage.class);
                log.info("REDIS-SUB: CHAT_MESSAGE from={} to={} thread={}", chatMessage.getSenderId(), chatMessage.getRecipientId(), chatMessage.getThreadId());
                
                // Broadcast to local STOMP subscribers
                String threadTopic = "/topic/chat/" + chatMessage.getThreadId();
                messagingTemplate.convertAndSend(threadTopic, chatMessage);

                List<String> notifyUsers = (List<String>) payload.get("notifyUsers");
                if (notifyUsers != null) {
                    for (String userId : notifyUsers) {
                        messagingTemplate.convertAndSend("/topic/inbox/" + userId, chatMessage);
                    }
                    log.info("REDIS-SUB: Broadcasted to {} and {} inboxes", threadTopic, notifyUsers.size());
                } else {
                    messagingTemplate.convertAndSend("/topic/inbox/" + chatMessage.getRecipientId(), chatMessage);
                    messagingTemplate.convertAndSend("/topic/inbox/" + chatMessage.getSenderId(), chatMessage);
                    log.info("REDIS-SUB: Broadcasted to {}, {}, {}", threadTopic, chatMessage.getRecipientId(), chatMessage.getSenderId());
                }
            } else if ("TYPING_EVENT".equals(type)) {
                Map<String, Object> typingData = (Map<String, Object>) data;
                String threadId = (String) typingData.get("threadId");
                log.info("REDIS-SUB: Broadcasting TYPING_EVENT to /topic/chat/{}/typing", threadId);
                messagingTemplate.convertAndSend("/topic/chat/" + threadId + "/typing", typingData);
            } else if ("CALL_SIGNAL".equals(type)) {
                Map<String, Object> callData = (Map<String, Object>) data;
                String recipientId = (String) callData.get("recipientId");
                log.info("REDIS-SUB: Broadcasting CALL_SIGNAL to /topic/call/{}", recipientId);
                messagingTemplate.convertAndSend("/topic/call/" + recipientId, callData);
            } else if ("READ_RECEIPT".equals(type)) {
                Map<String, Object> readData = (Map<String, Object>) data;
                String threadId = (String) readData.get("threadId");
                log.info("REDIS-SUB: Broadcasting READ_RECEIPT to /topic/chat/{}/read", threadId);
                messagingTemplate.convertAndSend("/topic/chat/" + threadId + "/read", readData);
            } else if ("MESSAGE_DELETED".equals(type)) {
                Map<String, Object> deleteData = (Map<String, Object>) data;
                String threadId = (String) deleteData.get("threadId");
                String userId = (String) deleteData.get("userId");
                log.info("REDIS-SUB: Broadcasting MESSAGE_DELETED to /topic/chat/{} and /topic/inbox/{}", threadId, userId);
                messagingTemplate.convertAndSend("/topic/chat/" + threadId, payload);
                messagingTemplate.convertAndSend("/topic/inbox/" + userId, payload);
            } else if ("THREAD_DELETED".equals(type)) {
                Map<String, Object> threadData = (Map<String, Object>) data;
                String userId = (String) threadData.get("userId");
                log.info("REDIS-SUB: Broadcasting THREAD_DELETED to /topic/inbox/{}", userId);
                messagingTemplate.convertAndSend("/topic/inbox/" + userId, payload);
            }
        } catch (Exception e) {
            log.error("REDIS SUB ERROR: ", e);
        }
    }
}
