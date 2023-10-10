package ru.netology.storagecloud.controllers;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.storagecloud.exceptions.BadCredentialsException;
import ru.netology.storagecloud.models.auth.requests.Login;
import ru.netology.storagecloud.models.auth.responses.AuthTokenResponse;
import ru.netology.storagecloud.services.tokens.LoginLogoutService;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class LoginLogoutController {

    protected final static String TOKEN_HEADER_NAME = "auth-token";

    private final LoginLogoutService loginLogoutService;

    @PermitAll
    @PostMapping("/login")
    public AuthTokenResponse login(@RequestBody Login login) throws BadCredentialsException {
        return new AuthTokenResponse(loginLogoutService.checkLogin(login));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        loginLogoutService.logout(request.getHeader(TOKEN_HEADER_NAME));
        response.addCookie(new Cookie("JSESSIONID", null));
    }
}
