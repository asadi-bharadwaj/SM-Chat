package com.SM.ChatService.repository;

import com.SM.ChatService.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * **Summary:** Repository interface for managing call logs.
 * 
 * **Flow:** Provides access to the call log history stored in the database. 
 * Interacts with {@link com.SM.ChatService.entity.CallLog} entities to track communication history between users.
 * 
 * **Features:** Call history tracking, missed call identification, and communication analytics.
 */
@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {
    
    /**
     * **Summary:** Retrieves call history for a specific user.
     * 
     * **Flow:** Executes a JPQL query to find all call logs where the specified user is either the caller or the receiver, 
     * ordered by timestamp in descending order to show recent calls first.
     * 
     * **Features:** User-specific call history retrieval for the "Recent Calls" UI.
     * 
     * @param userId The unique identifier of the user whose call logs are to be retrieved.
     * @return A list of {@link CallLog} entities representing the user's call history.
     */
    @Query("SELECT c FROM CallLog c WHERE c.callerId = :userId OR c.receiverId = :userId ORDER BY c.timestamp DESC")
    List<CallLog> findByUserIdOrderByTimestampDesc(String userId);
}
