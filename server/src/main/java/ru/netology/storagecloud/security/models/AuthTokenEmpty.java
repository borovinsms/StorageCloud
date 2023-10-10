package ru.netology.storagecloud.security.models;

public class AuthTokenEmpty implements SecurityToken {
    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getToken() {
        return "";
    }
}
