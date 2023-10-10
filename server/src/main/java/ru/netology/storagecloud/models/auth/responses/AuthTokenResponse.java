package ru.netology.storagecloud.models.auth.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthTokenResponse(

        @JsonProperty("auth-token")
        String authToken
) {
}
