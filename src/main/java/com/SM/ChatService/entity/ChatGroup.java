package com.SM.ChatService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary: Entity class representing a group chat room.
 * Flow: This entity is persisted in the "chat_groups" table and manages the metadata of a group chat, 
 * including its name, creator, and a unique thread identifier.
 * Features: Group chat creation, thread identification, and creation timestamp management.
 */
@Entity
@Table(name = "chat_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String threadId; // "GROUP_uuid"

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String creatorId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Summary: Lifecycle hook executed before the entity is persisted.
     * Flow: Checks if the createdAt timestamp is null; if so, initializes it with the current system time.
     * Features: Automatic timestamp generation for group creation.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
