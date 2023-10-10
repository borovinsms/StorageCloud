package ru.netology.storagecloud.models.auth.requests;

import lombok.Data;

@Data
public class Login {
    private String login, password;
}
