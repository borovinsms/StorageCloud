package ru.netology.storagecloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import ru.netology.storagecloud.config.entities.UserProperties;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.models.errors.ExceptionResponse;
import ru.netology.storagecloud.models.files.requests.NewFileName;
import ru.netology.storagecloud.models.files.responses.UserFileResponse;
import ru.netology.storagecloud.repositories.files.dao.jpa.FileJpaRepository;
import ru.netology.storagecloud.repositories.files.dao.entities.FileEntity;
import ru.netology.storagecloud.repositories.files.storage.FileStorage;
import ru.netology.storagecloud.repositories.tokens.jpa.TokenJpaRepository;
import ru.netology.storagecloud.repositories.tokens.util.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.entities.dao.TokenEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
class TestFileModule {

    private static final String TEST_LOGIN = "testLogin";
    private static final String TEST_PASSWORD = "testPassword";
    private static final String TOKEN_START_WITH = "Bearer ";
    private static final String TOKEN_HEADER_NAME = "auth-token";

    //    endpoints
    private static final String FILE_ENDPOINT = "/file";
    private static final String LIST_ENDPOINT = "/list";

    //    query params
    private static final String FILENAME_QUERY_PARAM = "filename";
    private static final String LIMIT_QUERY_PARAM = "limit";

    //    responses http codes
    private static final int CODE_INPUT_DATA_ERROR = 400;
    private static final int CODE_UNAUTHORIZED_ERROR = 401;
    private static final int CODE_INTERNAL_SERVER_ERROR = 500;

    //    token descriptions
    private static final String TOKEN_VALID = "token";
    private static final String TOKEN_WITHOUT_REQUIRED_START_STRING = "token without Bearer ";
    private static final String TOKEN_WITHOUT_SPACE = "token without space";
    private static final String TOKEN_ANY_STRING = "token any string";
    private static final String TOKEN_NON_EXISTENT_USER = "non-existent user";
    private static final String TOKEN_IS_NOT_ACTIVE = "is not active token";
    private static final String TOKEN_REPLACED = "replaced token";
    private static final String TOKEN_START_TIME_REPLACED = "replaced start";
    private static final String TOKEN_EXPIRATION_TIME_REPLACED = "replaced expiration";
    private static final String TOKEN_NULL_VALUE = "null";
    private static final String TOKEN_SKIP_THE_CHECK = "skip";

    //    test file
    private static final MockMultipartFile FILE = new MockMultipartFile(
            "file",
            "testFile.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "Test file text".getBytes()
    );

    //    error sources
    private static final String STORAGE_READ_FILE_EXCEPTION = "storage_read_file_exception";
    private static final String STORAGE_UPDATE_FILE_EXCEPTION = "storage_update_file_exception";
    private static final String STORAGE_DELETE_FILE_EXCEPTION = "storage_delete_file_exception";
    private static final String JPA_GET_FILE_EXCEPTION = "jpa_get_file_exception";
    private static final String JPA_GET_FILE_LIST_EXCEPTION = "jpa_get_file_list_exception";
    private static final String JPA_DELETE_FILE_EXCEPTION = "jpa_delete_file_exception";
    private static final String JPA_SAVE_FILE_EXCEPTION = "jpa_update_file_exception";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres");

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url=", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username=", POSTGRES::getUsername);
        registry.add("spring.datasource.password=", POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled=", () -> true);
        registry.add("security.users",
                () -> List.of(new UserProperties(
                        TEST_LOGIN,
                        TEST_PASSWORD,
                        true,
                        List.of("TEST"),
                        "TEST")
                )
        );
        registry.add("security.users[0].username", () -> TEST_LOGIN);
        registry.add("security.users[0].password", () -> TEST_PASSWORD);
        registry.add("security.users[0].credentials-expired", () -> true);
        registry.add("security.users[0].authorities", () -> null);
        registry.add("security.users[0].roles", () -> null);
    }

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private TokenGenerator tokenGenerator;
    @Autowired
    private TokenJpaRepository tokenJpaRepository;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private Filter springSecurityFilterChain;
    @MockBean
    private FileStorage fileStorage;
    @MockBean
    private FileJpaRepository fileJpaRepository;

    private MockMvc mockMvc;
    private String token;
    private TokenEntity tokenEntity;

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running StorageCloudApplicationTests");
        suiteStartTime = System.nanoTime();
        POSTGRES
                .withDatabaseName("postgres")
                .withUsername("test")
                .withPassword("test")
                .start();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("StorageCloudApplicationTests complete: " + (System.nanoTime() - suiteStartTime));
    }


    @BeforeEach
    public void initTest() {
        System.out.println("Starting new test");
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(springSecurityFilterChain).build();
        testStartTime = System.nanoTime();
        if (token == null) {
            final var generateToken = tokenGenerator.generateToken(TEST_LOGIN);
            tokenEntity = TokenEntity.builder()
                    .username(generateToken.getUsername())
                    .token(generateToken.getToken())
                    .start(generateToken.getStart())
                    .expiration(generateToken.getExpiration())
                    .isActive(true)
                    .build();
            token = TOKEN_START_WITH + tokenEntity.getToken();
        }
        Assertions.assertNotNull(tokenEntity);
        tokenJpaRepository.save(tokenEntity);
    }

    @AfterEach
    public void finalizeTest() {
        System.out.println("Test complete: " + (System.nanoTime() - testStartTime));
        SecurityContextHolder.clearContext();
    }

    @Test
    public void addFileSuccessTest() throws Exception {
        mockMvc
                .perform(
                        multipart(FILE_ENDPOINT)
                                .file(FILE)
                                .header(TOKEN_HEADER_NAME, token)
                                .queryParam(FILENAME_QUERY_PARAM, FILE.getOriginalFilename())
                )
                .andExpect(status().isOk());
    }

    @Test
    public void deleteFileSuccessTest() throws Exception {
        Mockito.when(fileJpaRepository.findByFileNameAndUsername(FILE.getOriginalFilename(), TEST_LOGIN))
                .thenReturn(Optional.of(new FileEntity()));
        mockMvc
                .perform(
                        delete(FILE_ENDPOINT)
                                .header(TOKEN_HEADER_NAME, token)
                                .queryParam(FILENAME_QUERY_PARAM, FILE.getOriginalFilename())
                )
                .andExpect(status().isOk());
    }

    @Test
    public void getFileSuccessTest() throws Exception {
        final var pathDirectoryFileInServer = "./path/dir";
        Mockito.when(fileJpaRepository.findByFileNameAndUsername(FILE.getOriginalFilename(), TEST_LOGIN))
                .thenReturn(
                        Optional.of(
                                FileEntity.builder()
                                        .pathDirectory(pathDirectoryFileInServer)
                                        .fileName(FILE.getOriginalFilename())
                                        .username(TEST_LOGIN)
                                        .build())
                );
        Mockito.when(fileStorage.readFile(pathDirectoryFileInServer, TEST_LOGIN, FILE.getOriginalFilename()))
                .thenReturn(FILE.getBytes());
        final var responseBody = mapper.writeValueAsString(new UserFileResponse("hash of file", FILE.getBytes()));

        mockMvc
                .perform(
                        get(FILE_ENDPOINT)
                                .header(TOKEN_HEADER_NAME, token)
                                .queryParam(FILENAME_QUERY_PARAM, FILE.getOriginalFilename())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @Test
    public void updateFileSuccessTest() throws Exception {
        final var newFileName = new NewFileName();
        newFileName.setFilename("new.name");
        Mockito.when(fileJpaRepository.findByFileNameAndUsername(FILE.getOriginalFilename(), TEST_LOGIN))
                .thenReturn(Optional.of(new FileEntity()));
        mockMvc
                .perform(
                        put(FILE_ENDPOINT)
                                .header(TOKEN_HEADER_NAME, token)
                                .queryParam(FILENAME_QUERY_PARAM, FILE.getOriginalFilename())
                                .content(mapper.writeValueAsString(newFileName))
                )
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("parametersForGetFileListTest")
    public void getFileListTest(int limitParameter) throws Exception {
        final var requestBuilder = get(LIST_ENDPOINT)
                .queryParam(LIMIT_QUERY_PARAM, String.valueOf(limitParameter))
                .header(TOKEN_HEADER_NAME, token);
        final var listFileEntities = new ArrayList<FileEntity>();
        final var responseBody = mapper.writeValueAsString(listFileEntities);
        Mockito.when(fileJpaRepository.getAllWithLimit(limitParameter, TEST_LOGIN)).thenReturn(listFileEntities);

        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andExpect(content().json(responseBody));
    }

    private static Stream<Arguments> parametersForGetFileListTest() {

        return Stream.of(
                Arguments.of(1),
                Arguments.of(5),
                Arguments.of(Integer.MAX_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersInternalServerErrorTest")
    public void internalServerErrorTest(
            MockHttpServletRequestBuilder requestBuilder,
            String errorMessage,
            String errorSource
    ) throws Exception {
        final var body = mapper.writeValueAsString(new ExceptionResponse(errorMessage, CODE_INTERNAL_SERVER_ERROR));
        requestBuilder.header(TOKEN_HEADER_NAME, token);
        switch (errorSource) {
            case STORAGE_READ_FILE_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileStorage).readFile(Mockito.any(), Mockito.any(), Mockito.any());
            case STORAGE_UPDATE_FILE_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileStorage).updateFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            case STORAGE_DELETE_FILE_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileStorage).deleteFile(Mockito.any(), Mockito.any(), Mockito.any());
            case JPA_GET_FILE_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileJpaRepository).findByFileNameAndUsername(Mockito.any(), Mockito.any());
            case JPA_SAVE_FILE_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileJpaRepository).save(Mockito.any());
            case JPA_DELETE_FILE_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileJpaRepository).delete(Mockito.any());
            case JPA_GET_FILE_LIST_EXCEPTION ->
                    Mockito.doThrow(new RuntimeException()).when(fileJpaRepository).getAllWithLimit(Mockito.anyInt(), Mockito.anyString());
        }

        mockMvc.perform(requestBuilder).andExpect(status().is(CODE_INTERNAL_SERVER_ERROR)).andExpect(content().json(body));
    }

    private static Stream<Arguments> parametersInternalServerErrorTest() throws JsonProcessingException {

        final var validLimitQueryParam = 3;
        final var fileNameValidQueryParam = "correct.fileName";
        final var newFileName = new NewFileName();
        newFileName.setFilename("correct.newFileName");
        final var newFileNameValidContent = new ObjectMapper().writeValueAsString(newFileName);

        return Stream.of(
                Arguments.of(
                        get(FILE_ENDPOINT)
                                .queryParam(FILENAME_QUERY_PARAM, fileNameValidQueryParam),
                        ErrorMessage.ERROR_GETTING_FILE,
                        STORAGE_READ_FILE_EXCEPTION
                ),
                Arguments.of(
                        get(FILE_ENDPOINT)
                                .queryParam(FILENAME_QUERY_PARAM, fileNameValidQueryParam),
                        ErrorMessage.ERROR_GETTING_FILE,
                        JPA_GET_FILE_EXCEPTION
                ),
                Arguments.of(
                        get(LIST_ENDPOINT)
                                .queryParam(LIMIT_QUERY_PARAM, String.valueOf(validLimitQueryParam)),
                        ErrorMessage.ERROR_GETTING_FILE_LIST,
                        JPA_GET_FILE_LIST_EXCEPTION
                ),
                Arguments.of(
                        delete(FILE_ENDPOINT)
                                .queryParam(FILENAME_QUERY_PARAM, fileNameValidQueryParam),
                        ErrorMessage.ERROR_DELETE_FILE,
                        STORAGE_DELETE_FILE_EXCEPTION
                ),
                Arguments.of(
                        delete(FILE_ENDPOINT)
                                .queryParam(FILENAME_QUERY_PARAM, fileNameValidQueryParam),
                        ErrorMessage.ERROR_DELETE_FILE,
                        JPA_DELETE_FILE_EXCEPTION
                ),
                Arguments.of(
                        put(FILE_ENDPOINT)
                                .queryParam(FILENAME_QUERY_PARAM, fileNameValidQueryParam)
                                .content(newFileNameValidContent),
                        ErrorMessage.ERROR_UPLOAD_FILE,
                        STORAGE_UPDATE_FILE_EXCEPTION
                ),
                Arguments.of(
                        put(FILE_ENDPOINT)
                                .queryParam(FILENAME_QUERY_PARAM, fileNameValidQueryParam)
                                .content(newFileNameValidContent),
                        ErrorMessage.ERROR_UPLOAD_FILE,
                        JPA_SAVE_FILE_EXCEPTION
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForRequestsErrorTest")
    public void requestsErrorTest(
            MockHttpServletRequestBuilder requestBuilder,
            int code,
            String message,
            String tokenValue
    ) throws Exception {
        switch (tokenValue) {
            case TOKEN_VALID:
                requestBuilder.header(TOKEN_HEADER_NAME, token);
                break;
            case TOKEN_WITHOUT_REQUIRED_START_STRING:
                requestBuilder.header(TOKEN_HEADER_NAME, token.split(" ")[1]);
                break;
            case TOKEN_WITHOUT_SPACE:
                requestBuilder.header(TOKEN_HEADER_NAME, String.join("", token.split(" ")));
                break;
            case TOKEN_ANY_STRING:
                requestBuilder.header(TOKEN_HEADER_NAME, "any string");
                break;
            case TOKEN_NON_EXISTENT_USER:
                requestBuilder.header(TOKEN_HEADER_NAME,
                        TOKEN_START_WITH + tokenGenerator.generateToken("non_existent_user").getToken());
                break;
            case TOKEN_IS_NOT_ACTIVE:
                final var notActiveTokenEntity = tokenJpaRepository.findById(TEST_LOGIN).orElseThrow();
                notActiveTokenEntity.setActive(false);
                tokenJpaRepository.save(notActiveTokenEntity);
                requestBuilder.header(TOKEN_HEADER_NAME, token);
                break;
            case TOKEN_REPLACED:
                requestBuilder.header(TOKEN_HEADER_NAME,
                        TOKEN_START_WITH + tokenGenerator.generateToken(TEST_LOGIN).getToken());
                break;
            case TOKEN_START_TIME_REPLACED:
                final var replacedStartTimeToken = tokenJpaRepository.findById(TEST_LOGIN).orElseThrow();
                replacedStartTimeToken.setStart(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                tokenJpaRepository.save(replacedStartTimeToken);
                requestBuilder.header(TOKEN_HEADER_NAME, token);
                break;
            case TOKEN_EXPIRATION_TIME_REPLACED:
                final var replacedExpirationTimeToken = tokenJpaRepository.findById(TEST_LOGIN).orElseThrow();
                final var start = LocalDateTime.now();
                final var expiration = LocalDateTime.from(start.plusDays(1));
                replacedExpirationTimeToken.setExpiration(expiration.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                tokenJpaRepository.save(replacedExpirationTimeToken);
                requestBuilder.header(TOKEN_HEADER_NAME, token);
                break;
            case TOKEN_NULL_VALUE:
                break;
            case TOKEN_SKIP_THE_CHECK:
                return;
            default:
                requestBuilder.header(TOKEN_HEADER_NAME, tokenValue);
                break;
        }

        final var body = mapper.writeValueAsString(new ExceptionResponse(message, code));
        mockMvc.perform(requestBuilder).andExpect(status().is(code)).andExpect(content().json(body));
    }

    private static Stream<Arguments> parametersForRequestsErrorTest() {

        final var correctFileName = "correct.fileName";

        final var arguments = new ArrayList<Arguments>();

        final var filenames = List.of(
                "",
                " ",
                "oneWordFileName",
                "more than two words",
                "twoWords withoutPoint",
                ".withTypeFileWithoutName",
                " .withTypeFileAndBlankName",
                ".with space in type",
                "with empty type.",
                "with space type. "
        );
        final var tokenDescriptions = List.of(
                TOKEN_NULL_VALUE,
                "",
                " ",
                TOKEN_WITHOUT_REQUIRED_START_STRING,
                TOKEN_WITHOUT_SPACE,
                TOKEN_ANY_STRING,
                TOKEN_NON_EXISTENT_USER,
                TOKEN_IS_NOT_ACTIVE,
                TOKEN_REPLACED,
                TOKEN_START_TIME_REPLACED,
                TOKEN_EXPIRATION_TIME_REPLACED
        );
//upload file request for http code 400
        arguments.addAll(filenames.stream().map(TestFileModule::uploadErrorInputData).toList());
//upload file request for http code 401
        arguments.addAll(tokenDescriptions.stream()
                .map(token -> uploadUnauthorizedError(token, correctFileName)).toList());
//download file request for http code 400
        arguments.addAll(filenames.stream().map(TestFileModule::downloadErrorInputData).toList());
//download file request for http code 401
        arguments.addAll(tokenDescriptions.stream()
                .map(token -> downloadUnauthorizedError(token, correctFileName)).toList());
//update file for http code 400
        final var mapper = new ObjectMapper();
        final var correctNewFileName = "correct.newFileName";
        final var validNewFileNameContent = getNewFileNameContent(mapper, correctNewFileName);
        arguments.addAll(filenames.stream()
                .map(filename -> updateErrorInputData(filename, validNewFileNameContent))
                .toList());
        arguments.addAll(filenames.stream()
                .map(filename -> {
                    if (!filename.isEmpty()) {
                        return updateErrorInputData(correctFileName, filename);
                    }
                    return updateUnauthorizedError(TOKEN_SKIP_THE_CHECK, correctFileName);
                })
                .toList());
        arguments.addAll(filenames.stream()
                .map(filename -> {
                    if (!filename.isEmpty()) {
                        return updateErrorInputData(correctFileName, getNewFileNameContent(mapper, filename));
                    }
                    return updateUnauthorizedError(TOKEN_SKIP_THE_CHECK, correctFileName);
                })
                .toList());
//update file request for http code 401
        arguments.addAll(tokenDescriptions.stream()
                .map(token -> updateUnauthorizedError(token, getNewFileNameContent(mapper, correctFileName))).toList());
//delete file request for http code 400
        arguments.addAll(filenames.stream().map(TestFileModule::deleteErrorInputData).toList());
//delete file request for http code 401
        arguments.addAll(tokenDescriptions.stream()
                .map(token -> deleteUnauthorizedError(token, correctFileName)).toList());
//get list request for http code 400
        final var invalidLimits = List.of(Integer.MIN_VALUE, -100, 0);
        arguments.addAll(invalidLimits.stream().map(TestFileModule::getListErrorInputData).toList());
//get list request for http code 401
        arguments.addAll(tokenDescriptions.stream().map(TestFileModule::getListUnauthorizedError).toList());

        return Stream.of(arguments.toArray(new Arguments[0]));
    }

    private static Arguments uploadErrorInputData(String filename) {
        return getErrorInputDataArg(
                multipart(FILE_ENDPOINT)
                        .file(FILE)
                        .queryParam(FILENAME_QUERY_PARAM, filename)
        );
    }

    private static Arguments uploadUnauthorizedError(String tokenDescription, String correctFileName) {
        return getUnauthorizedErrorArg(
                multipart(FILE_ENDPOINT)
                        .file(FILE)
                        .queryParam(FILENAME_QUERY_PARAM, correctFileName),
                tokenDescription
        );
    }

    private static Arguments downloadErrorInputData(String filename) {
        return getErrorInputDataArg(
                get(FILE_ENDPOINT)
                        .queryParam(FILENAME_QUERY_PARAM, filename)
        );
    }

    private static Arguments downloadUnauthorizedError(String tokenDescription, String correctFileName) {
        return getUnauthorizedErrorArg(
                get(FILE_ENDPOINT)
                        .queryParam(FILENAME_QUERY_PARAM, correctFileName),
                tokenDescription
        );
    }

    private static Arguments updateErrorInputData(String filename, String newFileName) {
        return getErrorInputDataArg(
                put(FILE_ENDPOINT)
                        .queryParam(FILENAME_QUERY_PARAM, filename)
                        .content(newFileName)
        );
    }

    private static Arguments updateUnauthorizedError(String tokenDescription, String correctFileName) {
        return getUnauthorizedErrorArg(
                put(FILE_ENDPOINT)
                        .queryParam(FILENAME_QUERY_PARAM, correctFileName)
                        .content(correctFileName),
                tokenDescription
        );
    }

    private static Arguments deleteErrorInputData(String filename) {
        return getErrorInputDataArg(
                delete(FILE_ENDPOINT)
                        .queryParam(FILENAME_QUERY_PARAM, filename)
        );
    }

    private static Arguments deleteUnauthorizedError(String tokenDescription, String correctFileName) {
        return getUnauthorizedErrorArg(
                delete(FILE_ENDPOINT)
                        .queryParam(FILENAME_QUERY_PARAM, correctFileName),
                tokenDescription
        );
    }

    private static Arguments getListErrorInputData(int limit) {
        return getErrorInputDataArg(
                get(LIST_ENDPOINT)
                        .queryParam(LIMIT_QUERY_PARAM, String.valueOf(limit))
        );
    }

    private static Arguments getListUnauthorizedError(String tokenDescription) {
        final var validLimit = 3;
        return getUnauthorizedErrorArg(
                get(LIST_ENDPOINT)
                        .queryParam(LIMIT_QUERY_PARAM, String.valueOf(validLimit)),
                tokenDescription
        );
    }

    private static Arguments getErrorInputDataArg(MockHttpServletRequestBuilder requestBuilder) {
        return Arguments.of(requestBuilder, CODE_INPUT_DATA_ERROR, ErrorMessage.ERROR_INPUT_DATA, TOKEN_VALID);
    }

    private static Arguments getUnauthorizedErrorArg(MockHttpServletRequestBuilder requestBuilder, String tokenDescription) {
        return Arguments.of(requestBuilder, CODE_UNAUTHORIZED_ERROR, ErrorMessage.UNAUTHORIZED_ERROR, tokenDescription);
    }

    private static String getNewFileNameContent(ObjectMapper mapper, String fileName) {
        final var newFileName = new NewFileName();
        newFileName.setFilename(fileName);
        try {
            return mapper.writeValueAsString(newFileName);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}