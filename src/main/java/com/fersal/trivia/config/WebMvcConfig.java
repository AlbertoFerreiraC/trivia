package com.fersal.trivia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${trivia.storage.images-dir:./storage/question-images}")
    private String imagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(imagesDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/questions/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}