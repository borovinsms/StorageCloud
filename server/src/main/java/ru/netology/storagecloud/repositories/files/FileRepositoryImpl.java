package ru.netology.storagecloud.repositories.files;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.responses.FileDescription;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.models.files.responses.UserFilesListResponse;
import ru.netology.storagecloud.repositories.files.dao.jpa.FileJpaRepository;
import ru.netology.storagecloud.repositories.files.dao.entities.FileEntity;
import ru.netology.storagecloud.repositories.files.storage.FileStorage;
import ru.netology.storagecloud.services.files.FileRepository;

import java.io.IOException;

@Data
@Repository
public class FileRepositoryImpl implements FileRepository {

    private final FileJpaRepository database;
    private final FileStorage storage;

    @Value("${storage.path}")
    private String path;


    @Override
    public UserFilesListResponse getFileList(FilesListParams params) {
        final var files = this.database.getAllWithLimit(params.count(), this.username());
        return new UserFilesListResponse(
                files.stream().map(f -> new FileDescription(f.getFileName(), f.getSize())).toList());
    }

    @Override
    public UserFileResponse getFile(GetFileParams params) throws IOException {
        final var fileEntity = fileEntity(params.filename());
        final var file = storage.readFile(
                fileEntity.getPathDirectory(),
                fileEntity.getUsername(),
                fileEntity.getFileName()
        );
        return new UserFileResponse("hash of file", file);

    }

    @Override
    public void deleteFile(DeleteFileParams params) throws IOException {
        final var fileEntity = fileEntity(params.fileName());
        storage.deleteFile(fileEntity.getPathDirectory(), fileEntity.getUsername(), fileEntity.getFileName());
        this.database.delete(fileEntity);
    }

    @Override
    public void updateFileName(UpdateFileNameParams params) throws IOException {
        final var fileEntity = fileEntity(params.fileName());
        storage.updateFile(
                fileEntity.getPathDirectory(),
                fileEntity.getUsername(),
                fileEntity.getFileName(),
                params.newName()
        );
        fileEntity.setFileName(params.newName());
        this.database.save(fileEntity);
    }

    @Override
    public void addFile(AddFileParams params) throws IOException {
        final var fileEntity = FileEntity.builder()
                .size(params.content().getBytes().length)
                .fileName(params.fileName())
                .username(username())
                .pathDirectory(this.path)
                .build();
        storage.saveFile(
                fileEntity.getPathDirectory(),
                fileEntity.getUsername(),
                fileEntity.getFileName(),
                params.content()
        );
        this.database.save(fileEntity);
    }

    private String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private FileEntity fileEntity(String filename) {
        return this.database.findByFileNameAndUsername(filename, username()).orElseThrow();
    }
}
