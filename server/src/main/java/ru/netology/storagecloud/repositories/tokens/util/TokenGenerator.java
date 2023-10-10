package ru.netology.storagecloud.repositories.tokens.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWEDecryptionKeySelector;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.netology.storagecloud.repositories.tokens.entities.models.AuthTokenGenerated;
import ru.netology.storagecloud.security.models.SecurityToken;
import ru.netology.storagecloud.security.util.SecurityTokenDecoder;
import ru.netology.storagecloud.services.tokens.models.AuthToken;
import ru.netology.storagecloud.services.tokens.util.AuthTokenDecoder;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Data
public class TokenGenerator implements AuthTokenDecoder, SecurityTokenDecoder {

    private final static byte[] ENCODER_SECRET = "SECRET_ENCODER_VALUE".getBytes();
    private final static JWEAlgorithm JWE_ALGORITHM = JWEAlgorithm.DIR;
    private final static EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A192GCM;
    private final static String USERNAME_CLAIM_KEY = "USERNAME_CLAIM_KEY";
    private final static String DATE_CLAIM_KEY = "DATE_CLAIM_KEY";
    private final static String EXPIRATION_CLAIM_KEY = "EXPIRATION_CLAIM_KEY";
    private final static int DAYS_TO_EXPIRATION = 1;

    private final DirectEncrypter encrypter;
    private final ImmutableSecret<SimpleSecurityContext> jweKeySource;
    @Value("${security.token.expiration: 1}")
    private int daysToExpiration;

    public TokenGenerator() throws KeyLengthException {
        final var bytes = new byte[24];
        for (int i = 0; i < ENCODER_SECRET.length; i++) {
            if (i < bytes.length) bytes[i] = ENCODER_SECRET[i];
        }
        this.encrypter = new DirectEncrypter(bytes);
        this.jweKeySource = new ImmutableSecret<>(bytes);
    }

    public AuthToken generateToken(String username) {
        try {
            final var start = LocalDateTime.now();
            final var startLong = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final var expiration = LocalDateTime.from(start.plusDays(this.daysToExpiration));
            final var expirationLong = expiration.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final var claims =
                    new JWTClaimsSet.Builder()
                            .claim(USERNAME_CLAIM_KEY, username)
                            .claim(DATE_CLAIM_KEY, startLong)
                            .claim(EXPIRATION_CLAIM_KEY, expirationLong)
                            .build();
            final var payload = new Payload(claims.toJSONObject());
            final var header = new JWEHeader(JWE_ALGORITHM, ENCRYPTION_METHOD);
            final var jweObject = new JWEObject(header, payload);
            jweObject.encrypt(encrypter);
            return AuthTokenGenerated.builder()
                    .token(jweObject.serialize())
                    .username(username)
                    .start(startLong)
                    .expiration(expirationLong)
                    .build();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthToken readAuthToken(String token) {
        return read(token);
    }

    @Override
    public SecurityToken readSecurityToken(String token) {
        return read(token);
    }

    private AuthTokenGenerated read(String string) {
        try {
            final var jwtProcessor = new DefaultJWTProcessor<SimpleSecurityContext>();
            final var jweKeySelector =
                    new JWEDecryptionKeySelector<>(JWE_ALGORITHM, ENCRYPTION_METHOD, this.jweKeySource);
            jwtProcessor.setJWEKeySelector(jweKeySelector);
            JWTClaimsSet claims = jwtProcessor.process(string, null);
            final var username = (String) claims.getClaim(USERNAME_CLAIM_KEY);
            final var start = (long) claims.getClaim(DATE_CLAIM_KEY);
            final var expiration = (long) claims.getClaim(EXPIRATION_CLAIM_KEY);
            return AuthTokenGenerated.builder()
                    .token(string)
                    .username(username)
                    .start(start)
                    .expiration(expiration)
                    .build();
        } catch (BadJOSEException | ParseException | JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
