package ru.netology.storagecloud.repositories;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.storagecloud.models.files.params.*;
import ru.netology.storagecloud.models.files.responses.FileDescription;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.models.files.responses.UserFilesListResponse;
import ru.netology.storagecloud.repositories.files.FileRepositoryImpl;
import ru.netology.storagecloud.repositories.files.dao.jpa.FileJpaRepository;
import ru.netology.storagecloud.repositories.files.dao.entities.FileEntity;
import ru.netology.storagecloud.repositories.files.storage.FileStorage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class TestFileRepositoryImpl {

    private static long suiteStartTime;
    private long testStartTime;

    @Value("${storage.path}")
    private String path;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running FileRepositoryImplClassTest");
        suiteStartTime = System.nanoTime();
        final var username = "testUser";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, ""));
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("FileRepositoryImplClassTest complete: " + (System.nanoTime() - suiteStartTime));
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
    public void getFileListMethodTest() {
        final var storage = Mockito.mock(FileStorage.class);
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var limit = 2;
        final var files = List.of(
                new FileDescription("file1", 1024),
                new FileDescription("file2", 1024 * 1024)
        );
        final var params = new FilesListParams(limit);
        Mockito.when(dataBase.getAllWithLimit(limit, username)).thenReturn(
                List.of(
                        FileEntity.builder().fileName(files.get(0).fileName()).username(username).size(files.get(0).size()).build(),
                        FileEntity.builder().fileName(files.get(1).fileName()).username(username).size(files.get(1).size()).build()
                )
        );
        final var response = new UserFilesListResponse(files);
        final var repository = new FileRepositoryImpl(dataBase, storage);
        final var result = repository.getFileList(params);
        Mockito.verify(dataBase, Mockito.times(1)).getAllWithLimit(limit, username);
        Assertions.assertEquals(response, result);
    }

    @Test
    public void getFileMethodTest() throws IOException {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var params = new GetFileParams(fileName);
        final var testFileBytes = new byte[0];
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.of(
                FileEntity.builder().fileName(fileName).username(username).build()
        ));
        Mockito.when(storage.readFile(path, username, fileName)).thenReturn(testFileBytes);
        final var response = new UserFileResponse("hash of file", testFileBytes);
        final var repository = new FileRepositoryImpl(dataBase, storage);
        final var result = repository.getFile(params);
        Assertions.assertEquals(response, result);
    }

    @Test
    public void getFileMethodNoEntityTest() throws IOException {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var params = new GetFileParams(fileName);
        final var testFileBytes = new byte[0];
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.empty());
        Mockito.when(storage.readFile(path, username, fileName)).thenReturn(testFileBytes);
        final var repository = new FileRepositoryImpl(dataBase, storage);
        Assertions.assertThrows(RuntimeException.class, () -> repository.getFile(params));
    }

    @Test
    public void deleteFileMethodTest() throws IOException {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var params = new DeleteFileParams(fileName);
        final var fileEntity = FileEntity.builder().fileName(fileName).username(username).build();
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.of(fileEntity));
        final var repository = new FileRepositoryImpl(dataBase, storage);
        final var directoryCaptor = ArgumentCaptor.forClass(String.class);
        final var userCaptor = ArgumentCaptor.forClass(String.class);
        final var fileCaptor = ArgumentCaptor.forClass(String.class);
        repository.deleteFile(params);
        Mockito.verify(storage, Mockito.times(1))
                .deleteFile(directoryCaptor.capture(), userCaptor.capture(), fileCaptor.capture());
        Mockito.verify(dataBase, Mockito.times(1)).delete(fileEntity);
        Assertions.assertEquals(this.path, directoryCaptor.getValue());
        Assertions.assertEquals(username, userCaptor.getValue());
        Assertions.assertEquals(fileName, fileCaptor.getValue());
    }

    @Test
    public void deleteFileMethodNoEntityTest() {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var params = new DeleteFileParams(fileName);
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.empty());
        final var repository = new FileRepositoryImpl(dataBase, storage);
        Assertions.assertThrows(RuntimeException.class, () -> repository.deleteFile(params));
    }

    @Test
    public void deleteFileWithStorageExceptionTest() throws IOException {
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var params = new DeleteFileParams(fileName);
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        Mockito.doThrow(new IOException()).when(storage).deleteFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.of(FileEntity.builder().build()));
        final var repository = new FileRepositoryImpl(dataBase, storage);

        Assertions.assertThrows(IOException.class, () -> repository.deleteFile(params));
        Mockito.verify(dataBase, Mockito.times(0)).delete(Mockito.any(FileEntity.class));
    }

    @Test
    public void updateFileMethodTest() throws IOException {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var newFileName = "newTestFileName";
        final var params = new UpdateFileNameParams(fileName, newFileName);
        final var fileEntity = FileEntity.builder().fileName(fileName).username(username).build();
        final var fileEntityExpected = FileEntity.builder().fileName(newFileName).username(username).build();
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.of(fileEntity));
        final var repository = new FileRepositoryImpl(dataBase, storage);
        final var directoryCaptor = ArgumentCaptor.forClass(String.class);
        final var userCaptor = ArgumentCaptor.forClass(String.class);
        final var fileCaptor = ArgumentCaptor.forClass(String.class);
        final var newFileCaptor = ArgumentCaptor.forClass(String.class);
        final var saveFileCaptor = ArgumentCaptor.forClass(FileEntity.class);
        repository.updateFileName(params);
        Mockito.verify(storage, Mockito.times(1))
                .updateFile(directoryCaptor.capture(), userCaptor.capture(), fileCaptor.capture(), newFileCaptor.capture());
        Mockito.verify(dataBase, Mockito.times(1)).save(saveFileCaptor.capture());
        Assertions.assertEquals(this.path, directoryCaptor.getValue());
        Assertions.assertEquals(username, userCaptor.getValue());
        Assertions.assertEquals(fileName, fileCaptor.getValue());
        Assertions.assertEquals(fileEntityExpected, saveFileCaptor.getValue());
    }

    @Test
    public void updateFileMethodNoEntityTest() {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var newFileName = "newTestFileName";
        final var params = new UpdateFileNameParams(fileName, newFileName);
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.empty());
        final var repository = new FileRepositoryImpl(dataBase, storage);
        Assertions.assertThrows(RuntimeException.class, () -> repository.updateFileName(params));
    }

    @Test
    public void updateFileWithStorageExceptionTest() throws IOException {
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var params = new UpdateFileNameParams(fileName, fileName);
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        Mockito.doThrow(new IOException()).when(storage).updateFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(dataBase.findByFileNameAndUsername(fileName, username)).thenReturn(Optional.of(FileEntity.builder().build()));
        final var repository = new FileRepositoryImpl(dataBase, storage);

        Assertions.assertThrows(IOException.class, () -> repository.updateFileName(params));
        Mockito.verify(dataBase, Mockito.times(0)).save(Mockito.any(FileEntity.class));
    }

    @Test
    public void addFileMethodTest() throws IOException {
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();
        final var fileName = "testFileName";
        final var multipartFile = Mockito.mock(MultipartFile.class);
        final var fileBytes = new byte[100];
        final var params = new AddFileParams(fileName, multipartFile);
        Mockito.when(multipartFile.getBytes()).thenReturn(fileBytes);
        final var fileEntityExpected = FileEntity.builder()
                .pathDirectory(this.path)
                .username(username)
                .fileName(fileName)
                .size(fileBytes.length)
                .build();
        final var repository = new FileRepositoryImpl(dataBase, storage);
        final var fileEntityCaptor = ArgumentCaptor.forClass(FileEntity.class);
        repository.addFile(params);
        Mockito.verify(storage, Mockito.times(1))
                .saveFile(this.path, username, fileName, multipartFile);
        Mockito.verify(dataBase, Mockito.times(1)).save(fileEntityCaptor.capture());
        Assertions.assertEquals(fileEntityExpected, fileEntityCaptor.getValue());
    }

    @Test
    public void addFileWithStorageExceptionTest() throws IOException {
        final var storage = Mockito.mock(FileStorage.class);
        final var dataBase = Mockito.mock(FileJpaRepository.class);
        final var fileName = "testFileName";
        final var multipartFile = Mockito.mock(MultipartFile.class);
        final var fileBytes = new byte[0];
        final var params = new AddFileParams(fileName, multipartFile);
        Mockito.when(multipartFile.getBytes()).thenReturn(fileBytes);
        Mockito.doThrow(new IOException()).when(storage).saveFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        final var repository = new FileRepositoryImpl(dataBase, storage);

        Assertions.assertThrows(IOException.class, () -> repository.addFile(params));
        Mockito.verify(dataBase, Mockito.times(0)).save(Mockito.any(FileEntity.class));
    }
}
