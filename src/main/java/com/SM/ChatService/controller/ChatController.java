package com.SM.ChatService.controller;

import com.SM.ChatService.entity.ChatGroup;
import com.SM.ChatService.entity.ChatGroupMember;
import com.SM.ChatService.entity.ChatMessage;
import com.SM.ChatService.repository.ChatGroupMemberRepository;
import com.SM.ChatService.repository.ChatGroupRepository;
import com.SM.ChatService.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import java.security.Principal;
import org.springframework.data.redis.core.RedisTemplate;

import com.SM.ChatService.entity.CallLog;
import com.SM.ChatService.repository.CallLogRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * REST and WebSocket Controller for managing chat messages, groups, and real-time interactions.
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    private final ChatMessageRepository repository;
    private final ChatGroupRepository groupRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final CallLogRepository callLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${internal.secret:defaultSecret}")
    private String internalSecret;

    public ChatController(ChatMessageRepository repository,
                          ChatGroupRepository groupRepository,
                          ChatGroupMemberRepository memberRepository,
                          CallLogRepository callLogRepository,
                          SimpMessagingTemplate messagingTemplate,
                          RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.callLogRepository = callLogRepository;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    private Long getAuthenticatedUserId(Principal principal) {
        try {
            return Long.valueOf(principal.getName());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/calls/history")
    public List<CallLog> getCallHistory(Principal principal) {
        String userId = String.valueOf(getAuthenticatedUserId(principal));
        return callLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @PostMapping("/calls/log")
    public ResponseEntity<Void> logCall(@RequestBody Map<String, Object> payload, Principal principal) {
        String callerId = (String) payload.get("callerId");
        String receiverId = (String) payload.get("receiverId");
        String type = (String) payload.get("type");
        String status = (String) payload.get("status");
        Integer duration = (Integer) payload.get("duration");

        // Find the most recent call log between these two users
        List<CallLog> recentLogs = callLogRepository.findByUserIdOrderByTimestampDesc(callerId);
        CallLog logToUpdate = null;
        for (CallLog log : recentLogs) {
            if ((log.getCallerId().equals(callerId) && log.getReceiverId().equals(receiverId)) ||
                (log.getCallerId().equals(receiverId) && log.getReceiverId().equals(callerId))) {
                // If it's within the last 5 minutes, update it
                if (log.getTimestamp().plusMinutes(5).isAfter(LocalDateTime.now())) {
                    logToUpdate = log;
                    break;
                }
            }
        }

        if (logToUpdate != null) {
            logToUpdate.setStatus(status);
            logToUpdate.setDuration(duration);
            callLogRepository.save(logToUpdate);
        } else {
            CallLog log = new CallLog();
            log.setCallerId(callerId);
            log.setReceiverId(receiverId);
            log.setType(type);
            log.setStatus(status);
            log.setDuration(duration);
            log.setTimestamp(LocalDateTime.now());
            callLogRepository.save(log);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/calls/trigger")
    public ResponseEntity<Void> triggerCall(@RequestBody Map<String, Object> payload, Principal principal) {
        String senderId = String.valueOf(getAuthenticatedUserId(principal));
        String recipientId = (String) payload.get("recipientId");
        String type = (String) payload.get("type");
        String threadId = (String) payload.get("threadId");

        CallLog callLog = new CallLog();
        callLog.setCallerId(senderId);
        callLog.setReceiverId(recipientId);
        callLog.setType(type);
        callLog.setStatus("MISSED");
        callLog.setTimestamp(LocalDateTime.now());
        callLogRepository.save(callLog);

        redisTemplate.convertAndSend("chat-channel", Map.of(
            "type", "CALL_SIGNAL",
            "data", Map.of(
                "type", "OFFER_TRIGGER",
                "senderId", senderId,
                "recipientId", recipientId,
                "callType", type,
                "threadId", threadId
            )
        ));

        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> body = Map.of(
                "recipientId", recipientId,
                "type", "PUSH",
                "title", "Incoming " + type.toLowerCase() + " call",
                "message", "From " + senderId,
                "url", "/messages/" + threadId
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            restTemplate.postForObject("http://localhost:8084/api/notifications", new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.error("CALL NOTIF FAIL: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/send")
    public ChatMessage sendMessageRest(@RequestBody ChatMessage message, Principal principal) {
        String userId = String.valueOf(getAuthenticatedUserId(principal));
        message.setSenderId(userId);
        message.setTimestamp(LocalDateTime.now());
        message.setIsRead(false);

        if (message.getThreadId() == null || message.getThreadId().isEmpty()) {
            String[] ids = {message.getSenderId(), message.getRecipientId()};
            Arrays.sort(ids);
            message.setThreadId(ids[0] + "_" + ids[1]);
        }

        ChatMessage saved = repository.save(message);

        List<String> notifyUsers;
        if (saved.getThreadId().startsWith("GROUP_")) {
            notifyUsers = memberRepository.findByThreadId(saved.getThreadId()).stream()
                    .map(ChatGroupMember::getUserId).collect(Collectors.toList());
        } else {
            notifyUsers = Arrays.asList(saved.getSenderId(), saved.getRecipientId());
        }

        redisTemplate.convertAndSend("chat-channel", Map.of(
            "type", "CHAT_MESSAGE",
            "data", saved,
            "notifyUsers", notifyUsers
        ));

        return saved;
    }

    @GetMapping("/threads/{userId}")
    public List<ChatMessage> getThreads(@PathVariable String userId) {
        log.info("CHAT: GET /chat/threads/{}", userId);
        
        List<String> groupThreadIds = memberRepository.findByUserId(userId).stream()
                .map(ChatGroupMember::getThreadId)
                .collect(Collectors.toList());
        
        if (groupThreadIds.isEmpty()) {
            groupThreadIds = Arrays.asList("EMPTY_LIST_FALLBACK");
        }
        
        List<ChatMessage> result = repository.findMessagesForUser(userId, groupThreadIds);
        log.info("CHAT: Returning {} messages", result.size());
        return result;
    }

    @PutMapping("/read/{threadId}/{userId}")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable String threadId, @PathVariable String userId) {
        List<ChatMessage> unread = repository.findByThreadIdOrderByTimestampAsc(threadId).stream()
                .filter(m -> !userId.equals(m.getSenderId()) && Boolean.FALSE.equals(m.getIsRead()))
                .collect(Collectors.toList());
                
        for (ChatMessage m : unread) {
            m.setIsRead(true);
            repository.save(m);
        }
        
        if (!unread.isEmpty()) {
            redisTemplate.convertAndSend("chat-channel", Map.of(
                "type", "READ_RECEIPT",
                "threadId", threadId,
                "readerId", userId
            ));
        }
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups/{userId}")
    public List<ChatGroup> getGroups(@PathVariable String userId) {
        List<String> groupThreadIds = memberRepository.findByUserId(userId).stream()
                .map(ChatGroupMember::getThreadId)
                .collect(Collectors.toList());
        
        return groupRepository.findAll().stream()
                .filter(g -> groupThreadIds.contains(g.getThreadId()))
                .collect(Collectors.toList());
    }

    @GetMapping("/history/{threadId}")
    public List<ChatMessage> getHistory(@PathVariable String threadId) {
        return repository.findByThreadIdOrderByTimestampAsc(threadId);
    }

    @PostMapping("/groups")
    public ChatGroup createGroup(@RequestBody Map<String, Object> payload, Principal principal) {
        String name = (String) payload.get("name");
        List<String> memberIds = (List<String>) payload.get("memberIds");
        String creatorId = principal.getName();
        
        if (!memberIds.contains(creatorId)) {
            memberIds.add(creatorId);
        }

        String threadId = "GROUP_" + UUID.randomUUID().toString();
        ChatGroup group = new ChatGroup();
        group.setName(name);
        group.setThreadId(threadId);
        group.setCreatorId(creatorId);
        group.setCreatedAt(LocalDateTime.now());
        
        ChatGroup saved = groupRepository.save(group);

        for (String mid : memberIds) {
            ChatGroupMember member = new ChatGroupMember();
            member.setThreadId(threadId);
            member.setUserId(mid);
            member.setJoinedAt(LocalDateTime.now());
            memberRepository.save(member);
        }

        return saved;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message, Principal principal) {
        String userId = String.valueOf(getAuthenticatedUserId(principal));
        message.setSenderId(userId);
        message.setTimestamp(LocalDateTime.now());
        message.setIsRead(false);

        if (message.getThreadId() == null || message.getThreadId().isEmpty()) {
            String[] ids = {message.getSenderId(), message.getRecipientId()};
            Arrays.sort(ids);
            message.setThreadId(ids[0] + "_" + ids[1]);
        }

        ChatMessage saved = repository.save(message);

        List<String> notifyUsers;
        if (saved.getThreadId().startsWith("GROUP_")) {
            notifyUsers = memberRepository.findByThreadId(saved.getThreadId()).stream()
                    .map(ChatGroupMember::getUserId).collect(Collectors.toList());
        } else {
            notifyUsers = Arrays.asList(saved.getSenderId(), saved.getRecipientId());
        }

        redisTemplate.convertAndSend("chat-channel", Map.of(
            "type", "CHAT_MESSAGE",
            "data", saved,
            "notifyUsers", notifyUsers
        ));
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        String userId = String.valueOf(getAuthenticatedUserId(principal));
        String threadId = (String) payload.get("threadId");
        Boolean isTyping = (Boolean) payload.get("isTyping");

        redisTemplate.convertAndSend("chat-channel", Map.of(
            "type", "TYPING_EVENT",
            "data", Map.of("threadId", threadId, "senderId", userId, "isTyping", isTyping)
        ));
    }

    @MessageMapping("/chat.call.signal")
    public void handleCallSignal(@Payload Map<String, Object> signal, Principal principal) {
        String senderId = String.valueOf(getAuthenticatedUserId(principal));
        String recipientId = (String) signal.get("recipientId");
        
        signal.put("senderId", senderId);

        redisTemplate.convertAndSend("chat-channel", Map.of(
            "type", "CALL_SIGNAL",
            "data", signal
        ));
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return exception.getMessage();
    }
}
