package ru.netology.storagecloud.repositories.tokens.entities.models;

import lombok.Builder;
import ru.netology.storagecloud.security.models.SecurityToken;
import ru.netology.storagecloud.services.tokens.models.AuthToken;

@Builder
public record AuthTokenGenerated(
        String token,
        String username,
        long start,
        long expiration

) implements AuthToken, SecurityToken {
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getExpiration() {
        return expiration;
    }
}
