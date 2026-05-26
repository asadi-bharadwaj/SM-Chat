package com.SM.ChatService.repository;

import com.SM.ChatService.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * **Summary:** Repository interface for managing chat groups.
 * 
 * **Flow:** Interacts with the database to manage {@link com.SM.ChatService.entity.ChatGroup} entities. 
 * Provides metadata management for group chats such as thread names and created dates.
 * 
 * **Features:** Group chat management, thread metadata retrieval.
 */
@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
    
    /**
     * **Summary:** Finds a chat group by its thread ID.
     * 
     * **Flow:** Queries the database for a chat group record matching the provided unique thread identifier.
     * 
     * **Features:** Thread-based group metadata lookup.
     * 
     * @param threadId The unique identifier of the chat thread.
     * @return An {@link java.util.Optional} containing the {@link ChatGroup} if found, or empty if not.
     */
    Optional<ChatGroup> findByThreadId(String threadId);
}
