package com.SM.ChatService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * **Summary:** Entity representing a single chat message.
 * 
 * **Flow:** Maps to the "chat_messages" table. It stores the content, sender, recipient, 
 * and status of messages in both direct and group chats. It includes sophisticated logic 
 * for soft-deletion per user (storing IDs in a comma-separated list) and tracking read status 
 * for real-time receipts.
 * 
 * **Features:** Message persistence, multi-user soft deletion, read tracking, 
 * and support for diverse message types (TEXT, VOICE, CALL).
 */
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

    private String type = "TEXT"; // TEXT, VOICE, CALL

    private String audioUrl;

    private Integer duration; // In seconds

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String threadId; // Unique ID for the conversation between two users

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(columnDefinition = "TEXT")
    private String deletedByUsers = ""; // Comma-separated user IDs

    private Boolean isDeletedForAll = false;

    /**
     * **Summary:** Lifecycle hook for default value initialization before persistence.
     * 
     * **Flow:** Ensures the timestamp is set and non-null defaults are established 
     * for read status and deletion flags.
     * 
     * **Features:** Entity data consistency.
     */
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (deletedByUsers == null) {
            deletedByUsers = "";
        }
        if (isDeletedForAll == null) {
            isDeletedForAll = false;
        }
    }
    
    /**
     * **Summary:** Custom getter for the read status.
     * 
     * @return true if the message has been read, false otherwise.
     */
    public Boolean getIsRead() {
        return isRead != null ? isRead : false;
    }
    
    /**
     * **Summary:** Custom setter for the read status.
     * 
     * @param isRead The new read status.
     */
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead != null ? isRead : false;
    }

    /**
     * **Summary:** Checks if this message has been deleted by a specific user.
     * 
     * **Flow:** First checks the global {@code isDeletedForAll} flag. If false, it parses 
     * the {@code deletedByUsers} string to see if the given user ID is present.
     * 
     * **Features:** User-specific message visibility control.
     * 
     * @param userId The unique identifier of the user to check for.
     * @return true if the message should be hidden from the user, false otherwise.
     */
    public boolean isDeletedFor(String userId) {
        if (Boolean.TRUE.equals(isDeletedForAll)) return true;
        if (deletedByUsers == null || deletedByUsers.isEmpty()) return false;
        return java.util.Arrays.asList(deletedByUsers.split(",")).contains(userId);
    }

    /**
     * **Summary:** Marks the message as deleted for a specific user.
     * 
     * **Flow:** appends the user ID to the {@code deletedByUsers} comma-separated list, 
     * ensuring no duplicates are added.
     * 
     * **Features:** Implementation of per-user soft deletion.
     * 
     * @param userId The identifier of the user who "deleted" the message.
     */
    public void addDeletedByUser(String userId) {
        if (deletedByUsers == null || deletedByUsers.isEmpty()) {
            deletedByUsers = userId;
        } else {
            java.util.List<String> ids = new java.util.ArrayList<>(java.util.Arrays.asList(deletedByUsers.split(",")));
            if (!ids.contains(userId)) {
                ids.add(userId);
                deletedByUsers = String.join(",", ids);
            }
        }
    }
}
