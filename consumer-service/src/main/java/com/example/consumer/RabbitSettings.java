package com.example.consumer;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("spring.rabbitmq")
public record RabbitSettings(@NotBlank String host, @Min(1) @Max(65535) int port,
        @NotBlank String username, @NotBlank String password, @NotBlank String virtualHost) {}
