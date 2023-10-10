package ru.netology.storagecloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.MultiValueMapAdapter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import ru.netology.storagecloud.config.entities.UserProperties;
import ru.netology.storagecloud.models.errors.ExceptionResponse;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.models.auth.responses.AuthTokenResponse;
import ru.netology.storagecloud.repositories.tokens.jpa.TokenJpaRepository;
import ru.netology.storagecloud.repositories.tokens.util.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.entities.dao.TokenEntity;
import ru.netology.storagecloud.services.tokens.util.AuthTokenDecoder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.testcontainers.junit.jupiter.Testcontainers
class TestLoginLogoutModule {

    private static final int PORT = 8000;
    private static final String HOST = "http://localhost:" + PORT + "/";
    private static final String TEST_LOGIN = "testLogin";
    private static final String TEST_PASSWORD = "testPassword";
    private static final String TOKEN_START_WITH = "Bearer ";
    private static final String TOKEN_HEADER_NAME = "auth-token";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres");

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url=", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username=", POSTGRES::getUsername);
        registry.add("spring.datasource.password=", POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled=", () -> true);
        registry.add("server.port", () -> PORT);
        registry.add("security.users",
                () -> List.of(new UserProperties(TEST_LOGIN, TEST_PASSWORD, true, List.of("TEST"), "TEST"))
        );
        registry.add("security.users[0].username", () -> TEST_LOGIN);
        registry.add("security.users[0].password", () -> TEST_PASSWORD);
        registry.add("security.users[0].credentials-expired", () -> true);
        registry.add("security.users[0].authorities", () -> null);
        registry.add("security.users[0].roles", () -> null);
    }

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private AuthTokenDecoder tokenDecoder;
    @Autowired
    private TokenGenerator tokenGenerator;
    @Autowired
    private TokenJpaRepository tokenJpaRepository;

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
        testStartTime = System.nanoTime();
    }

    @AfterEach
    public void finalizeTest() {
        System.out.println("Test complete: " + (System.nanoTime() - testStartTime));
    }

    @Test
    public void loginSuccessTest() throws JsonProcessingException {

        final var url = HOST + "login";
        final var login = new Login();
        login.setLogin(TEST_LOGIN);
        login.setPassword(TEST_PASSWORD);

        final var request = new RequestEntity<>(login, HttpMethod.POST, URI.create(url));
        final var entity = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        final var responseBody = mapper.readValue(entity.getBody(), AuthTokenResponse.class);
        final var token = tokenDecoder.readAuthToken(responseBody.authToken());
        Assertions.assertEquals(HttpStatusCode.valueOf(200), entity.getStatusCode());
        Assertions.assertNotNull(responseBody.authToken());
        Assertions.assertFalse(responseBody.authToken().isEmpty());
        Assertions.assertFalse(responseBody.authToken().isBlank());
        Assertions.assertNotNull(token);
        Assertions.assertEquals(token.getUsername(), TEST_LOGIN);
        Assertions.assertEquals(token.getToken(), responseBody.authToken());
        Assertions.assertTrue(token.getStart() > 0 && token.getExpiration() > 0 && token.getExpiration() > token.getStart());
    }

    @ParameterizedTest
    @MethodSource("parametersForLoginInvalidTest")
    public void loginInvalidTest(String username, String password) throws JsonProcessingException {

        final var url = HOST + "login";
        final var login = new Login();
        login.setLogin(username);
        login.setPassword(password);

        final var request = new RequestEntity<>(login, HttpMethod.POST, URI.create(url));
        final var entity = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        final var responseBody = mapper.readValue(entity.getBody(), ExceptionResponse.class);
        Assertions.assertEquals(HttpStatusCode.valueOf(400), entity.getStatusCode());
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(responseBody.id(), 400);
        Assertions.assertNotNull(responseBody.message());
    }

    private static Stream<Arguments> parametersForLoginInvalidTest() {

        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(TEST_LOGIN, null),
                Arguments.of(null, TEST_PASSWORD),
                Arguments.of("", TEST_PASSWORD),
                Arguments.of(TEST_LOGIN, " "),
                Arguments.of(TEST_LOGIN, "invalidPassword"),
                Arguments.of("invalidLogin", TEST_PASSWORD)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForLogoutTest")
    public void logoutTest(String authToken) {
        var token = (String) null;
        switch (authToken) {
            case "null":
                break;
            case "token":
                final var newToken = tokenGenerator.generateToken(TEST_LOGIN);
                final var tokenEntity = TokenEntity.builder()
                        .username(newToken.getUsername())
                        .token(newToken.getToken())
                        .start(newToken.getStart())
                        .expiration(newToken.getExpiration())
                        .isActive(true)
                        .build();
                tokenJpaRepository.save(tokenEntity);
                token = TOKEN_START_WITH + newToken.getToken();
                break;
            default:
                token = TOKEN_START_WITH + authToken;
                break;
        }

        final var url = HOST + "logout";
        final var headers = token == null ? null : new MultiValueMapAdapter<>(Map.of(TOKEN_HEADER_NAME, List.of(token)));
        final var request = new RequestEntity<>(headers, HttpMethod.POST, URI.create(url));
        final var response = restTemplate.exchange(url, HttpMethod.POST, request, Object.class);

        if (authToken.equals("token")) {
            final var savedToken = tokenJpaRepository.findById(TEST_LOGIN).orElse(null);
            Assertions.assertNotNull(savedToken);
            Assertions.assertFalse(savedToken.isActive());
        }

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }

    private static Stream<Arguments> parametersForLogoutTest() {

        return Stream.of(
                Arguments.of("token"),
                Arguments.of("anyString"),
                Arguments.of(""),
                Arguments.of("null"),
                Arguments.of(" ")
        );
    }
}