package ru.netology.storagecloud.models.files.responses;

import java.util.List;

public record UserFilesListResponse(List<FileDescription> files) {
}
