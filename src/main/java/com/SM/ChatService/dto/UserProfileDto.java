package com.SM.ChatService.dto;

import lombok.Data;

/**
 * **Summary:** Data Transfer Object (DTO) for user profile information.
 * 
 * **Flow:** Used to transfer user profile data between services or between layers of the Chat Service. 
 * It is typically populated from external user services and used to display user details in chat contexts.
 * 
 * **Features:** Supports user identification and display within the chat interface, 
 * including username, display name, and avatar URL.
 */
@Data
public class UserProfileDto {
    private Long id;
    private Long authUserId;
    private String username;
    private String displayName;
    private String avatarUrl;
}
