package com.SM.ChatService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * **Summary:** Entity representing a call history record.
 * 
 * **Flow:** Maps to the "call_logs" table in the database. It is used to persist metadata 
 * about voice and video calls initiated through the service, including participant IDs, 
 * call status, and duration.
 * 
 * **Features:** Call history persistence, communication tracking, and analytics support.
 */
@Entity
@Table(name = "call_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String callerId;

    @Column(nullable = false)
    private String receiverId;

    @Column(nullable = false)
    private String type; // VOICE, VIDEO

    @Column(nullable = false)
    private String status; // COMPLETED, MISSED, REJECTED, BUSY

    private Integer duration; // In seconds

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * **Summary:** Lifecycle hook called before the entity is persisted to the database.
     * 
     * **Flow:** Checks if the {@code timestamp} is null; if so, populates it with the current date and time.
     * 
     * **Features:** Automatic record timestamping.
     */
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
