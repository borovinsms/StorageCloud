package ru.netology.storagecloud.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.storagecloud.exceptions.InputDataException;
import ru.netology.storagecloud.exceptions.InternalServerException;
import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.responses.FileDescription;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.services.files.FileService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class FileController {

    private final FileService service;

    @GetMapping("/list")
    public List<FileDescription> getFileList(@RequestParam int limit) throws InternalServerException, InputDataException {
        return service.getFileList(new FilesListParams(limit)).files();
    }

    @GetMapping("/file")
    public UserFileResponse getFile(@RequestParam String filename) throws InputDataException, InternalServerException {
        return service.getFile(new GetFileParams(filename));
    }

    @DeleteMapping("/file")
    public void deleteFile(@RequestParam String filename) throws InternalServerException, InputDataException {
        service.deleteFile(new DeleteFileParams(filename));
    }

    @PutMapping("/file")
    public void updateFile(@RequestParam String filename, @RequestBody String newFileName) throws InternalServerException, InputDataException {
        service.updateFile(new UpdateFileNameParams(filename, newFileName));
    }

    @PostMapping("/file")
    public void addFile(@RequestParam String filename, @RequestPart MultipartFile file) throws InputDataException {
        service.addFile(new AddFileParams(filename, file));
    }
}
