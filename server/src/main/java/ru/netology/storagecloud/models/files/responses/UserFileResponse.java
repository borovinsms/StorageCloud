package ru.netology.storagecloud.models.files.responses;

public record UserFileResponse(String hash, byte[] file) {
}
