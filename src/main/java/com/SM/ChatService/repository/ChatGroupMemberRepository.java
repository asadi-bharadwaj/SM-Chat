package com.SM.ChatService.repository;

import com.SM.ChatService.entity.ChatGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * **Summary:** Repository interface for managing group members in a chat.
 * 
 * **Flow:** Interacts with the database to perform CRUD operations on {@link com.SM.ChatService.entity.ChatGroupMember} entities. 
 * Used by the chat service to manage and verify group memberships during message routing.
 * 
 * **Features:** Group chat management, member lookup, and membership verification.
 */
@Repository
public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {
    
    /**
     * **Summary:** Finds all members of a specific chat thread.
     * 
     * **Flow:** Queries the database for all membership records associated with the given thread ID.
     * 
     * **Features:** Thread-based member retrieval for message broadcasting.
     * 
     * @param threadId The unique identifier of the chat thread.
     * @return A list of {@link ChatGroupMember} entities belonging to the thread.
     */
    List<ChatGroupMember> findByThreadId(String threadId);

    /**
     * **Summary:** Finds all group memberships for a specific user.
     * 
     * **Flow:** Queries the database for all membership records associated with the given user ID.
     * 
     * **Features:** User-based group membership retrieval for populating user's chat list.
     * 
     * @param userId The unique identifier of the user.
     * @return A list of {@link ChatGroupMember} entities the user is part of.
     */
    List<ChatGroupMember> findByUserId(String userId);
}
