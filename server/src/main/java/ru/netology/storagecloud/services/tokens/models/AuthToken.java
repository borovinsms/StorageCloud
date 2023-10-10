package ru.netology.storagecloud.services.tokens.models;

public interface AuthToken {
    String getUsername();
    String getToken();
    long getStart();
    long getExpiration();
}
