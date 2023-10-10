package ru.netology.storagecloud.security.util;

import ru.netology.storagecloud.security.models.SecurityToken;

public interface SecurityTokenDecoder {

    SecurityToken readSecurityToken(String token);
}
