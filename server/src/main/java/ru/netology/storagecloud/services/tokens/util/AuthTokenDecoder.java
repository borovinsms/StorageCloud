package ru.netology.storagecloud.services.tokens.util;

import ru.netology.storagecloud.services.tokens.models.AuthToken;

public interface AuthTokenDecoder {

    AuthToken readAuthToken(String token);
}
