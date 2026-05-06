package com.SM.ChatService.repository;

import com.SM.ChatService.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByThreadIdOrderByTimestampAsc(String threadId);
    List<ChatMessage> findBySenderIdOrRecipientIdOrderByTimestampDesc(String senderId, String recipientId);
}
