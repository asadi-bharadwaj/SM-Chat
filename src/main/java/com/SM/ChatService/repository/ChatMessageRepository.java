package com.SM.ChatService.repository;

import com.SM.ChatService.entity.ChatMessage;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByThreadIdOrderByTimestampAsc(String threadId);

    /**
     * Latest message per thread for this user, sorted by recency. Bounded for inbox performance.
     */
    @Aggregation(pipeline = {
            "{ $match: { $or: [ { 'senderId': ?0 }, { 'recipientId': ?0 } ] } }",
            "{ $sort: { 'timestamp': -1 } }",
            "{ $group: { _id: '$threadId', doc: { $first: '$$ROOT' } } }",
            "{ $replaceRoot: { newRoot: '$doc' } }",
            "{ $sort: { 'timestamp': -1 } }",
            "{ $limit: 100 }"
    })
    List<ChatMessage> findInboxThreads(String userId);
}
