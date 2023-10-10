package ru.netology.storagecloud.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import ru.netology.storagecloud.exceptions.BadCredentialsException;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.services.tokens.LoginLogoutService;
import ru.netology.storagecloud.services.tokens.models.AuthToken;
import ru.netology.storagecloud.services.tokens.repository.TokenRepository;
import ru.netology.storagecloud.services.tokens.util.AuthTokenDecoder;

import java.util.stream.Stream;

public class TestLoginLogoutService {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running LoginLogoutServiceClassTest");
        suiteStartTime = System.nanoTime();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("LoginLogoutServiceClassTest complete: " + (System.nanoTime() - suiteStartTime));
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
    public void checkLoginMethodTest() throws BadCredentialsException {
        final var username = "testLogin";
        final var password = "testPassword";
        final var token = "testToken";
        final var decoder = Mockito.mock(AuthTokenDecoder.class);
        final var login = new Login();
        login.setLogin(username);
        login.setPassword(password);

        final var repository = Mockito.mock(TokenRepository.class);
        Mockito.when(repository.generateToken(login)).thenReturn(token);
        final var service = new LoginLogoutService(repository, decoder);
        final var result = service.checkLogin(login);
        Assertions.assertEquals(result, token);
    }

    @ParameterizedTest
    @MethodSource("parametersForCheckLoginMethodExceptionTest")
    public void checkLoginMethodExceptionTest(String tokenStr) {
        final var username = "testLogin";
        final var password = "testPassword";
        final var token = tokenStr.equals("null") ? null : tokenStr;
        final var decoder = Mockito.mock(AuthTokenDecoder.class);
        final var login = new Login();
        login.setLogin(username);
        login.setPassword(password);

        final var repository = Mockito.mock(TokenRepository.class);
        if (tokenStr.equals("throwable")) {
            Mockito.doThrow(new RuntimeException()).when(repository).generateToken(login);
        } else {
            Mockito.when(repository.generateToken(login)).thenReturn(token);
        }
        final var service = new LoginLogoutService(repository, decoder);
        Assertions.assertThrows(BadCredentialsException.class, () -> service.checkLogin(login));
    }

    private static Stream<Arguments> parametersForCheckLoginMethodExceptionTest() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of(" "),
                Arguments.of("null"),
                Arguments.of("throwable")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForLogoutMethodTest")
    public void logoutMethodTest(String token) {
        final var trimmedToken = token != null ? token.split(" ")[1].trim() : null;
        final var repository = Mockito.mock(TokenRepository.class);
        final var tokenDecoder = Mockito.mock(AuthTokenDecoder.class);
        final var authToken = Mockito.mock(AuthToken.class);
        Mockito
                .when(tokenDecoder.readAuthToken(trimmedToken))
                .thenReturn(authToken);
        final var service = new LoginLogoutService(repository, tokenDecoder);
        service.logout(token);
        Mockito.verify(tokenDecoder, Mockito.times(1)).readAuthToken(trimmedToken);
        Mockito.verify(repository, Mockito.times(1)).logout(authToken);
    }

    private static Stream<Arguments> parametersForLogoutMethodTest() {
        return Stream.of(
                Arguments.of("Bearer auth token"),
                Arguments.of("Bearer auth-token")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForLogoutMethodExceptionTest")
    public void logoutMethodExceptionTest(String tokenStr) {
        final var isDecoderThrowException = tokenStr.equals("decoder exception");
        final var isRepositoryThrowException = tokenStr.equals("repository exception");
        final var token = tokenStr.equals("null") ? null : tokenStr;
        final var trimmedToken =
                isDecoderThrowException || isRepositoryThrowException ? token.split(" ")[1].trim() : null;
        final var repository = Mockito.mock(TokenRepository.class);
        final var tokenDecoder = Mockito.mock(AuthTokenDecoder.class);
        final var authToken = Mockito.mock(AuthToken.class);
        if (isDecoderThrowException) {
            Mockito.doThrow(new RuntimeException()).when(tokenDecoder).readAuthToken(trimmedToken);
        }
        if (isRepositoryThrowException) {
            Mockito.when(tokenDecoder.readAuthToken(trimmedToken)).thenReturn(authToken);
            Mockito.doThrow(new RuntimeException()).when(repository).logout(authToken);
        }
        final var service = new LoginLogoutService(repository, tokenDecoder);
        service.logout(token);
        Assertions.assertDoesNotThrow(() -> service.logout(token));
    }

    private static Stream<Arguments> parametersForLogoutMethodExceptionTest() {
        return Stream.of(
                Arguments.of("null"),
                Arguments.of(" "),
                Arguments.of(""),
                Arguments.of("decoder exception"),
                Arguments.of("repository exception")
        );
    }
}
