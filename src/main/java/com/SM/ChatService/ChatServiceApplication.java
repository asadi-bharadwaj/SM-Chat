package com.SM.ChatService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Summary: Entry point for the Chat Service microservice.
 * Flow: Initializes the Spring Boot application context, enables service discovery via Eureka, 
 * and starts the embedded web server to handle chat-related requests.
 * Features: Microservice initialization, Service Discovery integration.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ChatServiceApplication {
    /**
     * Summary: Main method to launch the Chat Service.
     * Flow: Delegates to SpringApplication.run to bootstrap the application.
     * Features: Application startup.
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
