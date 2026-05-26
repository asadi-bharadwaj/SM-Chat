package com.SM.ChatService.repository;

import com.SM.ChatService.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * **Summary:** Repository interface for managing chat messages.
 * 
 * **Flow:** Handles database interactions for storing and retrieving {@link com.SM.ChatService.entity.ChatMessage} entities. 
 * It supports thread-based, user-based, and complex query-based message retrieval to support various chat views.
 * 
 * **Features:** Message history persistence, group chat history retrieval, and unified message inbox support.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * **Summary:** Retrieves all messages for a specific thread in chronological order.
     * 
     * **Flow:** Queries the database for all messages with the given thread ID, ordered by timestamp ascending 
     * to reconstruct a chat conversation.
     * 
     * **Features:** Chronological thread history retrieval.
     * 
     * @param threadId The unique identifier of the chat thread.
     * @return A list of {@link ChatMessage} entities belonging to the thread.
     */
    List<ChatMessage> findByThreadIdOrderByTimestampAsc(String threadId);
    
    /**
     * **Summary:** Retrieves a combined list of direct and group messages for a user.
     * 
     * **Flow:** Executes a custom JPQL query to find all messages where the user is either the sender or the recipient, 
     * or the message belongs to one of the user's group threads. Results are ordered by timestamp descending.
     * 
     * **Features:** Unified inbox retrieval for global message overview.
     * 
     * @param userId The unique identifier of the user.
     * @param groupThreadIds A list of thread IDs for groups the user belongs to.
     * @return A list of {@link ChatMessage} entities relevant to the user.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.senderId = :userId OR m.recipientId = :userId OR (m.threadId IS NOT NULL AND m.threadId IN :groupThreadIds) ORDER BY m.timestamp DESC")
    List<ChatMessage> findMessagesForUser(@Param("userId") String userId, @Param("groupThreadIds") List<String> groupThreadIds);
    
    /**
     * **Summary:** Retrieves messages involving a specific user, ordered by most recent.
     * 
     * **Flow:** Queries for all messages where the specified user is either the sender or the recipient, 
     * sorted by timestamp descending to show latest interactions first.
     * 
     * **Features:** User-centric message history retrieval.
     * 
     * @param senderId The user identifier as sender.
     * @param recipientId The user identifier as recipient.
     * @return A list of {@link ChatMessage} entities.
     */
    List<ChatMessage> findBySenderIdOrRecipientIdOrderByTimestampDesc(String senderId, String recipientId);
}
