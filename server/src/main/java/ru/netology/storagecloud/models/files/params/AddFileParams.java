package ru.netology.storagecloud.models.files.params;

import org.springframework.web.multipart.MultipartFile;

public record AddFileParams(String fileName, MultipartFile content) {
}
