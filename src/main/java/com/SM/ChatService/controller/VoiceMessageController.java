package com.SM.ChatService.controller;

import com.SM.ChatService.entity.ChatMessage;
import com.SM.ChatService.repository.ChatMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Summary: REST Controller for handling voice message uploads and processing.
 * Flow: Receives audio files via multipart requests, validates user authorization for the thread, 
 * stores the file on the filesystem, and returns an internal URL for access.
 * Features: Voice messaging, file system storage, thread-based authorization.
 */
@RestController
@RequestMapping("/chat/voice")
public class VoiceMessageController {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Summary: Constructor for VoiceMessageController.
     * @param chatMessageRepository Repository for chat message data access.
     */
    public VoiceMessageController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Summary: Extracts the authenticated user ID from the Principal object.
     * Flow: Parses the name from the Principal and converts it to a Long ID.
     * Features: User authentication context resolution.
     * @param principal The security principal of the authenticated user.
     * @return The authenticated user's ID as a Long.
     */
    private Long getAuthenticatedUserId(Principal principal) {
        return Long.valueOf(principal.getName());
    }

    /**
     * Summary: Handles the uploading of a voice message file.
     * Flow: 
     * 1. Validates that the file is not empty.
     * 2. Authorizes the user by checking if their ID is part of the thread ID.
     * 3. Ensures the upload directory exists.
     * 4. Generates a unique filename and saves the file to disk.
     * 5. Returns the internal URL of the saved audio file.
     * Features: Voice message recording upload, secure file storage, unique ID generation.
     * @param principal The authenticated user's principal.
     * @param file The multipart audio file to be uploaded.
     * @param threadId The ID of the chat thread the message belongs to.
     * @return ResponseEntity containing the audio URL on success, or an error message on failure.
     * @throws IOException if there's an error during file directory creation or file writing.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVoiceMessage(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("threadId") String threadId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Long userId = getAuthenticatedUserId(principal);
        
        // Security check: Ensure user belongs to the thread
        if (!threadId.contains(String.valueOf(userId))) {
            return ResponseEntity.status(403).body("Unauthorized access to thread");
        }

        try {
            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get("data/voice_messages");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".webm";
            String filename = userId + "_" + UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Generate internal URL (accessible via Gateway mapping we'll add later)
            String audioUrl = "/api/voice-messages/" + filename;

            return ResponseEntity.ok(Map.of("audioUrl", audioUrl));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }
}
