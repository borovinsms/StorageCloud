package ru.netology.storagecloud.repositories.tokens;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.services.tokens.models.AuthToken;
import ru.netology.storagecloud.repositories.tokens.jpa.TokenJpaRepository;
import ru.netology.storagecloud.repositories.tokens.util.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.entities.dao.TokenEntity;
import ru.netology.storagecloud.services.tokens.repository.TokenRepository;

@Repository
public class LoginLogoutRepository extends DaoAuthenticationProvider implements TokenRepository {

    protected final TokenJpaRepository tokenJpaRepository;
    protected final TokenGenerator tokenGenerator;

    public LoginLogoutRepository(
            TokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            TokenJpaRepository tokenJpaRepository) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.tokenGenerator = tokenGenerator;
        this.setPasswordEncoder(passwordEncoder);
        this.setUserDetailsService(userDetailsService);
    }

    @Override
    public String generateToken(Login login) {
        final var username = login.getLogin();
        final var authentication = new UsernamePasswordAuthenticationToken(username, login.getPassword());
        return generateToken(authentication).getToken();
    }

    @Override
    public void logout(AuthToken token) {
        final var tokenEntity = tokenJpaRepository.findById(token.getUsername()).orElse(null);
        logout(tokenEntity);
    }

    private AuthToken generateToken(UsernamePasswordAuthenticationToken authentication) {
        final var result = this.authenticate(authentication);
        final var token = tokenGenerator.generateToken(result.getName());
        final var tokenEntity = TokenEntity.builder()
                .username(token.getUsername())
                .token(token.getToken())
                .start(token.getStart())
                .expiration(token.getExpiration())
                .isActive(true)
                .build();
        this.tokenJpaRepository.save(tokenEntity);
        return token;
    }

    private void logout(TokenEntity tokenEntity) {
        if (tokenEntity != null) {
            tokenEntity.setActive(false);
            tokenJpaRepository.save(tokenEntity);
        }
        SecurityContextHolder.clearContext();
    }
}
