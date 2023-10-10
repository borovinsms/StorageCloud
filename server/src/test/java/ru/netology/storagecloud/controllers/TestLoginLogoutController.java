package ru.netology.storagecloud.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.models.auth.responses.AuthTokenResponse;
import ru.netology.storagecloud.services.tokens.LoginLogoutService;

import java.util.stream.Stream;

public class TestLoginLogoutController {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running LoginLogoutControllerClassTest");
        suiteStartTime = System.nanoTime();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("LoginLogoutControllerClassTest complete: " + (System.nanoTime() - suiteStartTime));
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
    public void loginMethodTest() throws ru.netology.storagecloud.exceptions.BadCredentialsException {
        final var service = Mockito.mock(LoginLogoutService.class);
        final var login = new Login();
        login.setLogin("testLogin");
        login.setPassword("testPassword");
        final var tokenText = "test-auth-token";
        Mockito.when(service.checkLogin(login)).thenReturn(tokenText);
        final var controller = new LoginLogoutController(service);
        final var argCaptor = ArgumentCaptor.forClass(Login.class);
        final var controllerResponse = controller.login(login);
        Mockito.verify(service, Mockito.times(1)).checkLogin(argCaptor.capture());
        Assertions.assertEquals(Login.class, argCaptor.getValue().getClass());
        Assertions.assertEquals(login, argCaptor.getValue());
        Assertions.assertEquals(controllerResponse, new AuthTokenResponse(tokenText));
    }

    @ParameterizedTest
    @MethodSource("parametersForLoginMethodExceptionsTest")
    public void loginMethodExceptionsTest(Exception e) throws ru.netology.storagecloud.exceptions.BadCredentialsException {
        final var service = Mockito.mock(LoginLogoutService.class);
        final var login = new Login();
        login.setLogin("testLogin");
        login.setPassword("testPassword");
        Mockito.doThrow(e).when(service).checkLogin(login);
        final var controller = new LoginLogoutController(service);
        Assertions.assertThrows(e.getClass(), () -> controller.login(login));
    }

    private static Stream<Arguments> parametersForLoginMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new BadCredentialsException("test"))
        );
    }

    @Test
    public void logoutMethodTest() {
        final var authRequestHeaderName = "auth-token";
        final var token = "auth-token";
        final var service = Mockito.mock(LoginLogoutService.class);
        final var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(authRequestHeaderName)).thenReturn(token);
        final var response = Mockito.mock(HttpServletResponse.class);
        final var controller = new LoginLogoutController(service);
        Assertions.assertDoesNotThrow(() -> controller.logout(request, response));
        Mockito.verify(service, Mockito.times(1)).logout(token);
    }

    @ParameterizedTest
    @MethodSource("parametersForLogoutMethodExceptionsTest")
    public void logoutMethodExceptionsTest(Exception e) {
        final var authRequestHeaderName = "auth-token";
        final var token = "auth-token";
        final var service = Mockito.mock(LoginLogoutService.class);
        final var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(authRequestHeaderName)).thenReturn(token);
        final var response = Mockito.mock(HttpServletResponse.class);
        Mockito.doThrow(e).when(service).logout(token);
        final var controller = new LoginLogoutController(service);
        Assertions.assertThrows(e.getClass(), () -> controller.logout(request, response));
    }

    private static Stream<Arguments> parametersForLogoutMethodExceptionsTest() {

        return Stream.of(
                Arguments.of(new RuntimeException("test"))
        );
    }
}
