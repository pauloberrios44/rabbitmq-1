package com.example.publisher;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("spring.datasource")
public record DatabaseSettings(@NotBlank String url, @NotBlank String username, @NotBlank String password) {}
