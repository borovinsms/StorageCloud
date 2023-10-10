package ru.netology.storagecloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.netology.storagecloud.exceptions.UnauthorizedException;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.repositories.tokens.entities.models.AuthTokenGenerated;
import ru.netology.storagecloud.repositories.tokens.util.TokenGenerator;
import ru.netology.storagecloud.security.models.SecurityToken;
import ru.netology.storagecloud.security.util.SecurityTokenDecoder;

import java.io.IOException;
import java.util.stream.Stream;

public class TestUsernameLoginFilter {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running UsernameLoginFilterClassTest");
        suiteStartTime = System.nanoTime();
        final var username = "testUser";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, ""));
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("UsernameLoginFilterClassTest complete: " + (System.nanoTime() - suiteStartTime));
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
    public void doFilterMethodTest() throws ServletException, IOException {
        final var request = Mockito.mock(HttpServletRequest.class);
        final var response = Mockito.mock(HttpServletResponse.class);
        final var filterChain = Mockito.mock(FilterChain.class);
        final var tokenGenerator = Mockito.mock(TokenGenerator.class);

        final var authFilter = new UsernameLoginFilter(tokenGenerator);

        authFilter.doFilter(request, response, filterChain);
        Mockito.verify(filterChain, Mockito.atLeastOnce()).doFilter(request, response);
    }

    @ParameterizedTest
    @MethodSource("parametersForAttemptAuthenticationMethodTest")
    public void attemptAuthenticationMethodTest(
            HttpServletRequest request,
            Authentication expected,
            AuthenticationManager authenticationManager
    ) {
        final var captor = ArgumentCaptor.forClass(Authentication.class);
        final var response = Mockito.mock(HttpServletResponse.class);
        final var decoder = Mockito.mock(SecurityTokenDecoder.class);
        final var username = "testUsername";
        final var token = "testToken";
        Mockito
                .when(decoder.readSecurityToken(Mockito.anyString()))
                .thenReturn(
                        AuthTokenGenerated.builder()
                                .username(username)
                                .token(token)
                                .build()
                );


        final var authFilter = new UsernameLoginFilter(decoder);
        if (authenticationManager != null) authFilter.setAuthenticationManager(authenticationManager);
        final var result = authFilter.attemptAuthentication(request, response);
        Assertions.assertEquals(result, expected);
        if (authenticationManager != null) {
            Mockito.verify(authenticationManager).authenticate(captor.capture());
            final var argument = captor.getValue();
            final var reqToken = new UsernamePasswordAuthenticationToken(username, token);
            Assertions.assertEquals(reqToken.getClass(), argument.getClass());
            Assertions.assertEquals(reqToken.getPrincipal(), argument.getPrincipal());
            Assertions.assertEquals(reqToken.getCredentials(), argument.getCredentials());
            Assertions.assertEquals(reqToken.isAuthenticated(), argument.isAuthenticated());
            Assertions.assertEquals(reqToken.getAuthorities(), argument.getAuthorities());
            Assertions.assertEquals(reqToken.getName(), argument.getName());
        }
    }

    private static Stream<Arguments> parametersForAttemptAuthenticationMethodTest() {

        final var nullRequest = Mockito.mock(HttpServletRequest.class);

        final var exceptionRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.doThrow(new RuntimeException()).when(exceptionRequest).getHeader(Mockito.anyString());

        final var username = "testUsername";
        final var token = "testToken";
        final var validRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(validRequest.getMethod()).thenReturn("POST");
        Mockito.when(validRequest.getHeader("auth-token")).thenReturn("Bearer " + token);

        final var expected = new UsernamePasswordAuthenticationToken(username, token);
        final var manager = Mockito.mock(AuthenticationManager.class);
        Mockito.when(manager.authenticate(Mockito.any(Authentication.class))).thenReturn(expected);

        return Stream.of(
                Arguments.of(nullRequest, null, null),
                Arguments.of(exceptionRequest, null, null),
                Arguments.of(validRequest, expected, manager)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForObtainUsernameAndPasswordMethodsTest")
    public void obtainUsernameAndPasswordMethodsTest(SecurityToken token, String expectedUsername, String expectedPassword) {
        final var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("auth-token")).thenReturn("Bearer testToken");
        final var decoder = Mockito.mock(SecurityTokenDecoder.class);
        Mockito.when(decoder.readSecurityToken(Mockito.anyString())).thenReturn(token);
        final var authFilter = new UsernameLoginFilter(decoder);

        final var resultUsername = authFilter.obtainUsername(request);
        final var resultPassword = authFilter.obtainPassword(request);
        Assertions.assertEquals(resultUsername, expectedUsername);
        Assertions.assertEquals(resultPassword, expectedPassword);
    }

    private static Stream<Arguments> parametersForObtainUsernameAndPasswordMethodsTest() {

        final var username = "testUsername";
        final var token = "testToken";
        final var decodingToken =
                AuthTokenGenerated.builder()
                        .username(username)
                        .token(token)
                        .build();

        return Stream.of(
                Arguments.of(decodingToken, username, token),
                Arguments.of(null, "", "")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForObtainUsernameExceptionsMethodsTest")
    public void obtainUsernameExceptionsMethodsTest(String token) {
        final var request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("auth-token")).thenReturn(!token.equals("null") ? token : null);
        final var decoder = Mockito.mock(SecurityTokenDecoder.class);
        if (token.startsWith("Bearer "))
            Mockito.doThrow(new RuntimeException()).when(decoder).readSecurityToken(Mockito.anyString());
        final var authFilter = new UsernameLoginFilter(decoder);
        Assertions
                .assertThrows(
                        UnauthorizedException.class,
                        () -> authFilter.obtainUsername(request),
                        ErrorMessage.UNAUTHORIZED_ERROR
                );
    }

    private static Stream<Arguments> parametersForObtainUsernameExceptionsMethodsTest() {

        final var token = "testToken";

        return Stream.of(
                Arguments.of(token),
                Arguments.of("null"),
                Arguments.of("Bearer " + token)
        );
    }
}
