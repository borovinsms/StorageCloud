package ru.netology.storagecloud.services.tokens;

import org.springframework.stereotype.Service;
import ru.netology.storagecloud.exceptions.BadCredentialsException;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.services.tokens.repository.TokenRepository;
import ru.netology.storagecloud.services.tokens.util.AuthTokenDecoder;

@Service
public class LoginLogoutService {

    protected final TokenRepository tokenRepository;
    protected final AuthTokenDecoder tokenDecoder;

    public LoginLogoutService(TokenRepository tokenRepository, AuthTokenDecoder tokenDecoder) {
        this.tokenRepository = tokenRepository;
        this.tokenDecoder = tokenDecoder;
    }

    public String checkLogin(Login login) throws BadCredentialsException {
        try {
            final var token = tokenRepository.generateToken(login);
            if (token == null || token.isEmpty() || token.isBlank()) {
                throw new BadCredentialsException(ErrorMessage.BAD_CREDENTIALS);
            }
            return token;
        } catch (Exception e) {
            throw new BadCredentialsException(ErrorMessage.BAD_CREDENTIALS);
        }
    }

    public void logout(String tokenString) {
        try {
            final var token = tokenDecoder.readAuthToken(tokenString.split(" ")[1].trim());
            tokenRepository.logout(token);
        } catch (Exception e) {
//            TODO nothing
        }
    }
}
