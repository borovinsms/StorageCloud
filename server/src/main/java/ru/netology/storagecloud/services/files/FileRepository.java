package ru.netology.storagecloud.services.files;

import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.models.files.responses.UserFilesListResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface FileRepository {

    UserFilesListResponse getFileList(FilesListParams params);

    UserFileResponse getFile(GetFileParams params) throws IOException, NoSuchAlgorithmException;

    void deleteFile(DeleteFileParams params) throws IOException;

    void updateFileName(UpdateFileNameParams params) throws IOException;

    void addFile(AddFileParams params) throws IOException;
}