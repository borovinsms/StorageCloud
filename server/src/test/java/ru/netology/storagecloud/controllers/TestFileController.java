package ru.netology.storagecloud.controllers;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.storagecloud.exceptions.InputDataException;
import ru.netology.storagecloud.exceptions.InternalServerException;
import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.responses.FileDescription;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.models.files.responses.UserFilesListResponse;
import ru.netology.storagecloud.services.files.FileService;

import java.util.List;
import java.util.stream.Stream;

public class TestFileController {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running FileControllerClassTest");
        suiteStartTime = System.nanoTime();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("FileControllerClassTest complete: " + (System.nanoTime() - suiteStartTime));
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

    @Test
    public void getFileListMethodTest() throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        final var serviceResponse = new UserFilesListResponse(
                List.of(
                        new FileDescription("file1", 1024),
                        new FileDescription("file2", 1024 * 1024)
                )
        );
        Mockito.when(service.getFileList(Mockito.any())).thenReturn(serviceResponse);
        final var controller = new FileController(service);
        final var limit = 2;
        final var argCaptor = ArgumentCaptor.forClass(FilesListParams.class);
        final var controllerResponse = controller.getFileList(limit);
        Mockito.verify(service, Mockito.times(1)).getFileList(argCaptor.capture());
        Assertions.assertEquals(controllerResponse, serviceResponse.files());
        Assertions.assertEquals(FilesListParams.class, argCaptor.getValue().getClass());
        Assertions.assertEquals(limit, argCaptor.getValue().count());
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileListMethodExceptionsTest")
    public void getFileListMethodExceptionsTest(Exception e) throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        Mockito.when(service.getFileList(Mockito.any())).thenThrow(e);
        final var controller = new FileController(service);
        final var limit = 2;
        Assertions.assertThrows(e.getClass(), () -> controller.getFileList(limit));
    }

    private static Stream<Arguments> parametersForGetFileListMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new InternalServerException("test")),
                Arguments.of(new InputDataException("test"))
        );
    }

    @Test
    public void getFileMethodTest() throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        final var serviceResponse = new UserFileResponse("hash", new byte[10]);
        Mockito.when(service.getFile(Mockito.any())).thenReturn(serviceResponse);
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        final var argCaptor = ArgumentCaptor.forClass(GetFileParams.class);
        final var controllerResponse = controller.getFile(fileName);
        Mockito.verify(service, Mockito.times(1)).getFile(argCaptor.capture());
        Assertions.assertEquals(controllerResponse, serviceResponse);
        Assertions.assertEquals(GetFileParams.class, argCaptor.getValue().getClass());
        Assertions.assertEquals(fileName, argCaptor.getValue().filename());
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileMethodExceptionsTest")
    public void getFileMethodExceptionsTest(Exception e) throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        Mockito.when(service.getFile(Mockito.any())).thenThrow(e);
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        Assertions.assertThrows(e.getClass(), () -> controller.getFile(fileName));
    }

    private static Stream<Arguments> parametersForGetFileMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new InternalServerException("test")),
                Arguments.of(new InputDataException("test"))
        );
    }

    @Test
    public void deleteFileMethodTest() throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        final var argCaptor = ArgumentCaptor.forClass(DeleteFileParams.class);
        controller.deleteFile(fileName);
        Mockito.verify(service, Mockito.times(1)).deleteFile(argCaptor.capture());
        Assertions.assertEquals(DeleteFileParams.class, argCaptor.getValue().getClass());
        Assertions.assertEquals(fileName, argCaptor.getValue().fileName());
    }

    @ParameterizedTest
    @MethodSource("parametersForDeleteFileMethodExceptionsTest")
    public void deleteFileMethodExceptionsTest(Exception e) throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        Mockito.doThrow(e).when(service).deleteFile(Mockito.any());
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        Assertions.assertThrows(e.getClass(), () -> controller.deleteFile(fileName));
    }

    private static Stream<Arguments> parametersForDeleteFileMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new InternalServerException("test")),
                Arguments.of(new InputDataException("test"))
        );
    }

    @Test
    public void updateFileMethodTest() throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        final var newFileName = "newFileNameTest";
        final var argCaptor = ArgumentCaptor.forClass(UpdateFileNameParams.class);
        controller.updateFile(fileName, newFileName);
        Mockito.verify(service, Mockito.times(1)).updateFile(argCaptor.capture());
        Assertions.assertEquals(UpdateFileNameParams.class, argCaptor.getValue().getClass());
        Assertions.assertEquals(fileName, argCaptor.getValue().fileName());
        Assertions.assertEquals(newFileName, argCaptor.getValue().newName());
    }

    @ParameterizedTest
    @MethodSource("parametersForUpdateFileMethodExceptionsTest")
    public void updateFileMethodExceptionsTest(Exception e) throws InternalServerException, InputDataException {
        final var service = Mockito.mock(FileService.class);
        Mockito.doThrow(e).when(service).updateFile(Mockito.any());
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        final var newFileName = "newFileNameTest";
        Assertions.assertThrows(e.getClass(), () -> controller.updateFile(fileName, newFileName));
    }

    private static Stream<Arguments> parametersForUpdateFileMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new InternalServerException("test")),
                Arguments.of(new InputDataException("test"))
        );
    }

    @Test
    public void addFileMethodTest() throws InputDataException {
        final var service = Mockito.mock(FileService.class);
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        final var multipartFile = Mockito.mock(MultipartFile.class);
        final var argCaptor = ArgumentCaptor.forClass(AddFileParams.class);
        controller.addFile(fileName, multipartFile);
        Mockito.verify(service, Mockito.times(1)).addFile(argCaptor.capture());
        Assertions.assertEquals(AddFileParams.class, argCaptor.getValue().getClass());
        Assertions.assertEquals(fileName, argCaptor.getValue().fileName());
        Assertions.assertEquals(multipartFile, argCaptor.getValue().content());
    }

    @ParameterizedTest
    @MethodSource("parametersForAddFileMethodExceptionsTest")
    public void addFileMethodExceptionsTest(Exception e) throws InputDataException {
        final var service = Mockito.mock(FileService.class);
        Mockito.doThrow(e).when(service).addFile(Mockito.any());
        final var controller = new FileController(service);
        final var fileName = "testFileName";
        final var multipartFile = Mockito.mock(MultipartFile.class);
        Assertions.assertThrows(e.getClass(), () -> controller.addFile(fileName, multipartFile));
    }

    private static Stream<Arguments> parametersForAddFileMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new InputDataException("test"))
        );
    }
}
