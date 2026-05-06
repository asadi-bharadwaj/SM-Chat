package com.SM.ChatService.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@CompoundIndexes({
        @CompoundIndex(name = "idx_sender_timestamp", def = "{'senderId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_recipient_timestamp", def = "{'recipientId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_thread_timestamp", def = "{'threadId': 1, 'timestamp': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    private String senderId;

    private String recipientId;

    private String content;

    private LocalDateTime timestamp;

    private String threadId; // Unique ID for the conversation between two users

    private Boolean isRead = false;
    
    // Standard getters/setters that Jackson and Lombok will both understand
    public Boolean getIsRead() {
        return isRead != null ? isRead : false;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead != null ? isRead : false;
    }
}
