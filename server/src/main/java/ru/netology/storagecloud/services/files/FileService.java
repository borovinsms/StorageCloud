package ru.netology.storagecloud.services.files;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.netology.storagecloud.exceptions.InputDataException;
import ru.netology.storagecloud.exceptions.InternalServerException;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.requests.NewFileName;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.models.files.responses.UserFilesListResponse;

@RequiredArgsConstructor
@Service
public class FileService {

    private final FileRepository repository;

    public UserFilesListResponse getFileList(FilesListParams params) throws InternalServerException, InputDataException {
        if (params.count() <= 0) throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
        try {
            return repository.getFileList(params);
        } catch (Exception e) {
            throw new InternalServerException(ErrorMessage.ERROR_GETTING_FILE_LIST);
        }
    }

    public UserFileResponse getFile(GetFileParams params) throws InputDataException, InternalServerException {
        checkFileName(params.filename());
        try {
            return repository.getFile(params);
        } catch (Exception e) {
            throw new InternalServerException(ErrorMessage.ERROR_GETTING_FILE);
        }
    }

    public void deleteFile(DeleteFileParams params) throws InputDataException, InternalServerException {
        checkFileName(params.fileName());
        try {
            repository.deleteFile(params);
        } catch (Exception e) {
            throw new InternalServerException(ErrorMessage.ERROR_DELETE_FILE);
        }
    }

    public void updateFile(UpdateFileNameParams params) throws InputDataException, InternalServerException {
        try {
            checkFileName(params.fileName());
            if (params.newName() == null) throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
            final var mapper = new ObjectMapper();
            final var newName = mapper.readValue(params.newName(), NewFileName.class);
            checkFileName(newName.getFilename());
            repository.updateFileName(new UpdateFileNameParams(params.fileName(), newName.getFilename()));
        } catch (InputDataException e) {
            throw e;
        } catch (JsonProcessingException | NullPointerException e) {
            throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
        } catch (Exception e) {
            throw new InternalServerException(ErrorMessage.ERROR_UPLOAD_FILE);
        }
    }

    public void addFile(AddFileParams params) throws InputDataException {
        checkFileName(params.fileName());
        if (params.content() == null) throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
        try {
            repository.addFile(params);
        } catch (Exception e) {
            throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
        }
    }

    private void checkFileName(String fileName) throws InputDataException {
        if (fileName == null || fileName.isBlank())
            throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
        final var partsFileName = fileName.split("\\.");
        if (
                partsFileName.length != 2
                        || partsFileName[0] == null
                        || partsFileName[0].isBlank()
                        || partsFileName[1] == null
                        || partsFileName[1].split(" ").length > 1
                        || partsFileName[1].isBlank()
        ) {
            throw new InputDataException(ErrorMessage.ERROR_INPUT_DATA);
        }
    }
}
