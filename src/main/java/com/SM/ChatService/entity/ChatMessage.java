package com.SM.ChatService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String threadId; // Unique ID for the conversation between two users

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
    }
    
    // Standard getters/setters that Jackson and Lombok will both understand
    public Boolean getIsRead() {
        return isRead != null ? isRead : false;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead != null ? isRead : false;
    }
}
