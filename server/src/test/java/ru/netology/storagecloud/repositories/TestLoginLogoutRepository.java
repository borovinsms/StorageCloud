package ru.netology.storagecloud.repositories;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.repositories.tokens.LoginLogoutRepository;
import ru.netology.storagecloud.repositories.tokens.entities.dao.TokenEntity;
import ru.netology.storagecloud.repositories.tokens.entities.models.AuthTokenGenerated;
import ru.netology.storagecloud.repositories.tokens.jpa.TokenJpaRepository;
import ru.netology.storagecloud.repositories.tokens.util.TokenGenerator;
import ru.netology.storagecloud.services.tokens.models.AuthToken;

import java.util.ArrayList;
import java.util.Optional;

public class TestLoginLogoutRepository {

    private static long suiteStartTime;
    private long testStartTime;

    @BeforeAll
    public static void initSuite() {
        System.out.println("Running LoginLogoutRepositoryClassTest");
        suiteStartTime = System.nanoTime();
    }

    @AfterAll
    public static void completeSuite() {
        System.out.println("LoginLogoutRepositoryClassTest complete: " + (System.nanoTime() - suiteStartTime));
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
    public void generateTokenMethodTest() {
        final var username = "test login";
        final var password = "test password";
        final var login = new Login();
        login.setLogin(username);
        login.setPassword(password);
        final var token = AuthTokenGenerated.builder()
                .username(username)
                .token(password)
                .build();
        final var jpaRepository = Mockito.mock(TokenJpaRepository.class);
        final var generator = Mockito.mock(TokenGenerator.class);
        Mockito.when(generator.generateToken(username)).thenReturn(token);
        final var encoder = Mockito.mock(PasswordEncoder.class);
        Mockito.when(encoder.matches(password, password)).thenReturn(true);
        final var userDetails = new User(username, password, new ArrayList<>());
        final var userDetailsService = Mockito.mock(UserDetailsService.class);
        Mockito.when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        final var repository = new LoginLogoutRepository(generator, encoder, userDetailsService, jpaRepository);
        final var resultAuthToken = repository.generateToken(login);
        final var captor = ArgumentCaptor.forClass(TokenEntity.class);
        Mockito.verify(jpaRepository, Mockito.times(1)).save(captor.capture());
        final var resultTokenEntity = captor.getValue();
        Assertions.assertEquals(resultTokenEntity.getUsername(), username);
        Assertions.assertEquals(resultTokenEntity.getToken(), password);
        Assertions.assertTrue(resultTokenEntity.isActive());
        Assertions.assertEquals(resultTokenEntity.getStart(), 0);
        Assertions.assertEquals(resultTokenEntity.getExpiration(), 0);
        Assertions.assertEquals(resultAuthToken, token.getToken());
    }

    @Test
    public void logoutMethodTest() {
        final var username = "test username";
        final var tokenEntity = Mockito.mock(TokenEntity.class);
        final var authToken = Mockito.mock(AuthToken.class);
        Mockito.when(authToken.getUsername()).thenReturn(username);
        final var generator = Mockito.mock(TokenGenerator.class);
        final var encoder = Mockito.mock(PasswordEncoder.class);
        final var userDetailsService = Mockito.mock(UserDetailsService.class);
        final var jpaRepository = Mockito.mock(TokenJpaRepository.class);
        Mockito.when(jpaRepository.findById(username)).thenReturn(Optional.of(tokenEntity));
        final var repository = new LoginLogoutRepository(generator, encoder, userDetailsService, jpaRepository);

        repository.logout(authToken);
        Mockito.verify(tokenEntity, Mockito.times(1)).setActive(false);
        Mockito.verify(jpaRepository, Mockito.times(1)).save(tokenEntity);
    }
}
