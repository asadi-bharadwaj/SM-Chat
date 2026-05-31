package com.SM.ChatService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * **Summary:** Web MVC configuration for the Chat Service.
 * 
 * **Flow:** Implements {@link WebMvcConfigurer} to customize Spring MVC behavior. 
 * Its primary responsibility is to configure resource handlers that allow the service 
 * to serve static content, such as uploaded voice messages, directly from the server's filesystem.
 * 
 * **Features:** Static resource serving, external directory exposure for media files.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * **Summary:** Registers handlers for serving static resources via HTTP.
     * 
     * **Flow:** Calls {@link #exposeDirectory(String, ResourceHandlerRegistry)} to map 
     * the local "data/voice_messages" directory to the "/voice-messages/**" URL pattern.
     * 
     * **Features:** Media resource mapping.
     * 
     * @param registry The registry used to define resource handlers.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory("data/voice_messages", registry);
    }

    /**
     * **Summary:** Helper method to map a local filesystem directory to a public resource handler.
     * 
     * **Flow:** 
     * 1. Resolves the absolute path of the provided directory name.
     * 2. Formats the path as a file-based resource location.
     * 3. Registers the location under the "/voice-messages/**" pattern.
     * 
     * **Features:** Dynamic path resolution for external storage.
     * 
     * @param dirName The relative path of the directory to expose.
     * @param registry The {@link ResourceHandlerRegistry} to register the handler with.
     */
    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        String projectRoot = System.getProperty("user.dir");
        // user.dir is .../Chat-Service/chat-service
        Path uploadDir = Paths.get(projectRoot).getParent().getParent().resolve(dirName).normalize();
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        
        if (!uploadPath.endsWith("/")) uploadPath += "/";

        registry.addResourceHandler("/voice-messages/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
