package com.SM.ChatService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * **Summary:** Entity representing a user's membership in a chat group.
 * 
 * **Flow:** Maps to the "chat_group_members" table. It acts as a join table with additional metadata 
 * to link users to specific chat threads, enabling group-based communication and access control.
 * 
 * **Features:** Group membership tracking and membership history.
 */
@Entity
@Table(name = "chat_group_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String threadId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /**
     * **Summary:** Lifecycle hook called before the entity is persisted to the database.
     * 
     * **Flow:** Checks if the {@code joinedAt} timestamp is null; if so, populates it with the current date and time.
     * 
     * **Features:** Automatic join-date timestamping.
     */
    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
