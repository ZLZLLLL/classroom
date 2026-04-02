package com.classroom.repository.mongo;

import com.classroom.entity.mongo.AiChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AiChatMessageRepository extends MongoRepository<AiChatMessageDocument, String> {

    List<AiChatMessageDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AiChatMessageDocument> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
}

