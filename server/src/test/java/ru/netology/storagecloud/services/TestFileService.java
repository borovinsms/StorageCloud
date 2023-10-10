package ru.netology.storagecloud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.storagecloud.exceptions.InputDataException;
import ru.netology.storagecloud.exceptions.InternalServerException;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.requests.NewFileName;
import ru.netology.storagecloud.models.files.responses.FileDescription;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.models.files.responses.UserFilesListResponse;
import ru.netology.storagecloud.services.files.FileRepository;
import ru.netology.storagecloud.services.files.FileService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestFileService {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running FileServiceClassTest");
        suiteStartTime = System.nanoTime();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("FileServiceClassTest complete: " + (System.nanoTime() - suiteStartTime));
    }

    @BeforeEach
    public void initTest() {
        System.out.println("Starting new test");
        testStartTime = System.nanoTime();
    }

    @AfterEach
    public void finalizeTest() {
        System.out.println("Test complete: " + (System.nanoTime() - testStartTime));
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileListMethodTest")
    public void getFileListMethodTest(UserFilesListResponse response) throws InternalServerException, InputDataException {
        final var params = new FilesListParams(1);
        final var repository = Mockito.mock(FileRepository.class);
        Mockito.when(repository.getFileList(params)).thenReturn(response);
        final var service = new FileService(repository);
        final var result = service.getFileList(params);
        Assertions.assertEquals(result, response);
    }

    private static Stream<Arguments> parametersForGetFileListMethodTest() {

        return Stream.of(
                Arguments.of(new UserFilesListResponse(new ArrayList<>())),
                Arguments.of(new UserFilesListResponse(null)),
                Arguments.of(new UserFilesListResponse(List.of(
                        new FileDescription("file1", 0),
                        new FileDescription("file2", 100),
                        new FileDescription("file3", 999),
                        new FileDescription("file4", 2),
                        new FileDescription("file5", 999999)
                )))
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileListInputDataExceptionMethodTest")
    public void getFileListInputDataExceptionMethodTest(FilesListParams params) {
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        Assertions.assertThrows(InputDataException.class, () -> service.getFileList(params), ErrorMessage.ERROR_INPUT_DATA);
    }

    private static Stream<Arguments> parametersForGetFileListInputDataExceptionMethodTest() {

        return Stream.of(
                Arguments.of(new FilesListParams(0)),
                Arguments.of(new FilesListParams(-1)),
                Arguments.of(new FilesListParams(-999999))
        );
    }

    @Test
    public void getFileListInternalServerExceptionMethodTest() {
        final var params = new FilesListParams(1);
        final var repository = Mockito.mock(FileRepository.class);
        Mockito.doThrow(new RuntimeException()).when(repository).getFileList(params);
        final var service = new FileService(repository);
        Assertions
                .assertThrows(
                        InternalServerException.class,
                        () -> service.getFileList(params),
                        ErrorMessage.ERROR_GETTING_FILE_LIST
                );
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileMethodTest")
    public void getFileMethodTest(UserFileResponse response) throws InternalServerException, InputDataException, IOException, NoSuchAlgorithmException {
        final var params = new GetFileParams("testFileName.test");
        final var repository = Mockito.mock(FileRepository.class);
        Mockito.when(repository.getFile(params)).thenReturn(response);
        final var service = new FileService(repository);
        final var result = service.getFile(params);
        Assertions.assertEquals(result, response);
    }

    private static Stream<Arguments> parametersForGetFileMethodTest() {

        return Stream.of(
                Arguments.of(new UserFileResponse("hash1", new byte[0])),
                Arguments.of(new UserFileResponse("hash2", new byte[1])),
                Arguments.of((UserFileResponse) null),
                Arguments.of(new UserFileResponse("hash3", new byte[999999]))
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileInputDataExceptionMethodTest")
    public void getFileInputDataExceptionMethodTest(GetFileParams params) {
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        Assertions.assertThrows(InputDataException.class, () -> service.getFile(params), ErrorMessage.ERROR_INPUT_DATA);
    }

    private static Stream<Arguments> parametersForGetFileInputDataExceptionMethodTest() {

        return Stream.of(
                Arguments.of(new GetFileParams(null)),
                Arguments.of(new GetFileParams("")),
                Arguments.of(new GetFileParams("   .  ")),
                Arguments.of(new GetFileParams(" ")),
                Arguments.of(new GetFileParams("invalidFileNameWithoutFileType"))
        );
    }

    @Test
    public void getFileInternalServerExceptionMethodTest() throws IOException, NoSuchAlgorithmException {
        final var params = new GetFileParams("testFileName.test");
        final var repository = Mockito.mock(FileRepository.class);
        Mockito.doThrow(new RuntimeException()).when(repository).getFile(params);
        final var service = new FileService(repository);
        Assertions
                .assertThrows(
                        InternalServerException.class,
                        () -> service.getFile(params),
                        ErrorMessage.ERROR_GETTING_FILE
                );
    }

    @Test
    public void deleteFileMethodTest() throws InternalServerException, InputDataException, IOException {
        final var params = new DeleteFileParams("testFileName.test");
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        service.deleteFile(params);
        Mockito.verify(repository, Mockito.times(1)).deleteFile(params);
    }

    @ParameterizedTest
    @MethodSource("parametersForDeleteFileInputDataExceptionMethodTest")
    public void deleteFileInputDataExceptionMethodTest(DeleteFileParams params) {
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        Assertions.assertThrows(InputDataException.class, () -> service.deleteFile(params), ErrorMessage.ERROR_INPUT_DATA);
    }

    private static Stream<Arguments> parametersForDeleteFileInputDataExceptionMethodTest() {

        return Stream.of(
                Arguments.of(new DeleteFileParams(null)),
                Arguments.of(new DeleteFileParams("")),
                Arguments.of(new DeleteFileParams("   .  ")),
                Arguments.of(new DeleteFileParams(" ")),
                Arguments.of(new DeleteFileParams("invalidFileNameWithoutFileType"))
        );
    }

    @Test
    public void deleteFileInternalServerExceptionMethodTest() throws IOException {
        final var params = new DeleteFileParams("testFileName.test");
        final var repository = Mockito.mock(FileRepository.class);
        Mockito.doThrow(new RuntimeException()).when(repository).deleteFile(params);
        final var service = new FileService(repository);
        Assertions
                .assertThrows(
                        InternalServerException.class,
                        () -> service.deleteFile(params),
                        ErrorMessage.ERROR_DELETE_FILE
                );
    }

    @Test
    public void updateFileMethodTest() throws IOException, InternalServerException, InputDataException {
        final var params = new UpdateFileNameParams("file1.test", "{\"filename\":\"new1.test\"}");
        final var mapper = new ObjectMapper();
        final var newName = mapper.readValue(params.newName(), NewFileName.class);
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        final var captor = ArgumentCaptor.forClass(UpdateFileNameParams.class);
        service.updateFile(params);
        Mockito.verify(repository, Mockito.times(1)).updateFileName(captor.capture());
        final var result = captor.getValue();
        Assertions.assertEquals(result.fileName(), params.fileName());
        Assertions.assertEquals(result.newName(), newName.getFilename());
    }

    @ParameterizedTest
    @MethodSource("parametersForUpdateFileInputDataExceptionMethodTest")
    public void updateFileInputDataExceptionMethodTest(UpdateFileNameParams params) {
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        Assertions.assertThrows(InputDataException.class, () -> service.updateFile(params), ErrorMessage.ERROR_INPUT_DATA);
    }

    private static Stream<Arguments> parametersForUpdateFileInputDataExceptionMethodTest() {

        return Stream.of(
                Arguments.of(new UpdateFileNameParams(null, "{\"filename\":\"new1.test\"}")),
                Arguments.of(new UpdateFileNameParams("", "{\"filename\":\"new1.test\"}")),
                Arguments.of(new UpdateFileNameParams("   .  ", "{\"filename\":\"new1.test\"}")),
                Arguments.of(new UpdateFileNameParams(" ", "{\"filename\":\"new1.test\"}")),
                Arguments.of(new UpdateFileNameParams("invalidFileNameWithoutFileType", "{\"filename\":\"new1.test\"}")),
                Arguments.of(new UpdateFileNameParams("fileName.test", null)),
                Arguments.of(new UpdateFileNameParams("fileName.test", "")),
                Arguments.of(new UpdateFileNameParams("fileName.test", "new1.test")),
                Arguments.of(new UpdateFileNameParams("fileName.test", "{\"filename\":null}")),
                Arguments.of(new UpdateFileNameParams("fileName.test", "{\"filename\":\"\"}")),
                Arguments.of(new UpdateFileNameParams("fileName.test", "{\"filename\":\" . \"}")),
                Arguments.of(new UpdateFileNameParams("fileName.test", "{\"filename\":\" \"}")),
                Arguments.of(new UpdateFileNameParams(" ", "{\"fileName\":\"new1.test\"}")),
                Arguments.of(new UpdateFileNameParams("fileName.test", "{\"filename\":\"invalidFileNameWithoutFileType\"}"))
        );
    }

    @Test
    public void updateFileInternalServerExceptionMethodTest() throws IOException {
        final var params = new UpdateFileNameParams("file1.test", "{\"filename\":\"new1.test\"}");
        final var repository = Mockito.mock(FileRepository.class);
        Mockito.doThrow(new RuntimeException()).when(repository).updateFileName(Mockito.any(UpdateFileNameParams.class));
        final var service = new FileService(repository);
        Assertions
                .assertThrows(
                        InternalServerException.class,
                        () -> service.updateFile(params),
                        ErrorMessage.ERROR_UPLOAD_FILE
                );
    }

    @Test
    public void addFileMethodTest() throws IOException, InputDataException {
        final var params = new AddFileParams("file1.test", Mockito.mock(MultipartFile.class));
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        service.addFile(params);
        Mockito.verify(repository, Mockito.times(1)).addFile(params);
    }

    @ParameterizedTest
    @MethodSource("parametersForAddFileInputDataExceptionMethodTest")
    public void addFileInputDataExceptionMethodTest(AddFileParams params) {
        final var repository = Mockito.mock(FileRepository.class);
        final var service = new FileService(repository);
        Assertions.assertThrows(InputDataException.class, () -> service.addFile(params), ErrorMessage.ERROR_INPUT_DATA);
    }

    private static Stream<Arguments> parametersForAddFileInputDataExceptionMethodTest() {

        final var multipartFile = Mockito.mock(MultipartFile.class);

        return Stream.of(
                Arguments.of(new AddFileParams("file1.test", null)),
                Arguments.of(new AddFileParams(null, multipartFile)),
                Arguments.of(new AddFileParams("", multipartFile)),
                Arguments.of(new AddFileParams(" . ", multipartFile)),
                Arguments.of(new AddFileParams(" ", multipartFile)),
                Arguments.of(new AddFileParams("invalidFileNameWithoutFileType", multipartFile))
        );
    }
}
