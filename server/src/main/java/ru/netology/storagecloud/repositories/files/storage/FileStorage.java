package ru.netology.storagecloud.repositories.files.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileStorage {

    public byte[] readFile(String directory, String username, String file) throws IOException {
        return Files.readAllBytes(Path.of(directory, username, file));
    }

    public void deleteFile(String directory, String username, String file) throws IOException {
        Files.delete(Path.of(directory, username, file));
    }

    public void saveFile(String directory, String username, String fileName, MultipartFile content) throws IOException {
        checkDirectory(directory);
        checkDirectory(directory + username + "/");
        final var pathFile = Path.of(directory, username, fileName);
        if (Files.notExists(pathFile)) Files.createFile(pathFile);
        Files.write(pathFile, content.getBytes());
    }

    public void updateFile(String directory, String username, String file, String newFileName) throws IOException {
        final var path = Path.of(directory, username, file);
        final var newPath = Path.of(directory, username, newFileName);
        Files.move(path, newPath);
    }

    private void checkDirectory(String name) throws IOException {
        final var path = Path.of(name);
        if (Files.notExists(path) || !Files.isDirectory(path)) {
            Files.createDirectory(path);
        }
    }
}
