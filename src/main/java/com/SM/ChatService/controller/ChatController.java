package com.SM.ChatService.controller;

import com.SM.ChatService.entity.ChatMessage;
import com.SM.ChatService.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@Controller
public class ChatController {

    private final ChatMessageRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatMessageRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    // REST Endpoints
    @GetMapping("/chat/history/{threadId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String threadId) {
        return repository.findByThreadIdOrderByTimestampAsc(threadId);
    }

    @GetMapping("/chat/threads/{userId}")
    @ResponseBody
    public List<ChatMessage> getUserThreads(@PathVariable String userId) {
        return repository.findInboxThreads(userId);
    }

    @PutMapping("/chat/read/{threadId}/{recipientId}")
    @ResponseBody
    public void markAsRead(@PathVariable String threadId, @PathVariable String recipientId) {
        List<ChatMessage> unread = repository.findByThreadIdOrderByTimestampAsc(threadId);
        unread.forEach(m -> {
            // Use Boolean.TRUE.equals to safely compare wrapper types
            if (m.getRecipientId().equals(recipientId) && !Boolean.TRUE.equals(m.getIsRead())) {
                m.setIsRead(true);
                repository.save(m);
            }
        });
    }

    // STOMP Message Handler
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        System.out.println("CORE: Received message from " + message.getSenderId() + " to " + message.getRecipientId());
        
        try {
            message.setTimestamp(LocalDateTime.now());
            // Ensure isRead is false for new messages
            message.setIsRead(false);
            
            if (message.getThreadId() == null || message.getThreadId().isEmpty()) {
                String[] ids = {String.valueOf(message.getSenderId()), String.valueOf(message.getRecipientId())};
                Arrays.sort(ids);
                message.setThreadId(ids[0] + "_" + ids[1]);
            }

            ChatMessage saved = repository.save(message);
            
            // Broadcast to the thread topic
            String threadTopic = "/topic/chat/" + saved.getThreadId();
            messagingTemplate.convertAndSend(threadTopic, saved);

            // Broadcast to individual inboxes
            String recipientInbox = "/topic/inbox/" + message.getRecipientId();
            String senderInbox = "/topic/inbox/" + message.getSenderId();
            
            messagingTemplate.convertAndSend(recipientInbox, saved);
            messagingTemplate.convertAndSend(senderInbox, saved);
            
            System.out.println("CORE: Broadcasted message ID=" + saved.getId());

        } catch (Exception e) {
            System.err.println("CORE ERROR: " + e.getMessage());
        }
    }
}
