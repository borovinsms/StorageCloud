package ru.netology.storagecloud.config.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public record UserProperties(
        @NotBlank
        String username,
        @NotBlank
        String password,
        @NotNull
        boolean credentialsExpired,
        List<String> authorities,
        String... roles
) {
}
