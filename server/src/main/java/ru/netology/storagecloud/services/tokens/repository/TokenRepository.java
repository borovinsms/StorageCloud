package ru.netology.storagecloud.services.tokens.repository;

import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.services.tokens.models.AuthToken;

public interface TokenRepository {

    String generateToken(Login login);

    void logout(AuthToken token);
}
