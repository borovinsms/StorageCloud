package ru.netology.storagecloud.security;

import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import ru.netology.storagecloud.exceptions.UnauthorizedException;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.repositories.tokens.jpa.TokenJpaRepository;
import ru.netology.storagecloud.services.tokens.util.AuthTokenDecoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

public class AuthTokenProvider implements AuthenticationProvider {

    protected final TokenJpaRepository tokenJpaRepository;
    protected AuthTokenDecoder tokenDecoder;

    public AuthTokenProvider(TokenJpaRepository tokenJpaRepository, AuthTokenDecoder tokenDecoder) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.tokenDecoder = tokenDecoder;
    }

    @SneakyThrows
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            final var tokenString = authentication.getCredentials().toString();
            final var token = tokenDecoder.readAuthToken(tokenString);
            final var tokenEntity = tokenJpaRepository.findById(token.getUsername()).orElse(null);
            final var nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (
                    tokenEntity == null
                            || !tokenEntity.isActive()
                            || !tokenEntity.getToken().equals(token.getToken())
                            || !tokenEntity.getUsername().equals(token.getUsername())
                            || tokenEntity.getStart() != token.getStart()
                            || tokenEntity.getExpiration() != token.getExpiration()
                            || tokenEntity.getExpiration() < nowTime
            ) {
                throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ERROR);
            }
            return new UsernamePasswordAuthenticationToken(tokenEntity.getUsername(), tokenEntity.getToken(), new ArrayList<>());
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ERROR);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
