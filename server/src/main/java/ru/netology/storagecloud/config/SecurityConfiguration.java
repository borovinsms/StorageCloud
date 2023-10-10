package ru.netology.storagecloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.netology.storagecloud.config.entities.UserProperties;
import ru.netology.storagecloud.models.errors.ErrorMessage;
import ru.netology.storagecloud.models.errors.ExceptionResponse;
import ru.netology.storagecloud.repositories.tokens.util.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.jpa.TokenJpaRepository;
import ru.netology.storagecloud.security.AuthTokenProvider;
import ru.netology.storagecloud.security.UsernameLoginFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
@ConfigurationProperties("security")
@Data
public class SecurityConfiguration implements WebMvcConfigurer {

    private final List<UserProperties> users = new ArrayList<>();
    private final List<String> origins = new ArrayList<>();

    @Bean
    public PasswordEncoder encoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TokenJpaRepository tokenJpaRepository,
            TokenGenerator tokenGenerator
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling().authenticationEntryPoint((request, response, authException) -> this.sendError(response))
                .and()
                .addFilterBefore(tokenFilter(tokenGenerator, tokenJpaRepository), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/logout").permitAll()
                .anyRequest().authenticated()
                .and()
                .logout().disable();
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource, PasswordEncoder encoder) {
        final var jdbcUserDetailsManager = new JdbcUserDetailsManager(dataSource);
        if (!users.isEmpty()) {
            for (var userProperty : users) {
                final var authorities = userProperty.authorities();
                final var roles = userProperty.roles();
                final var userBuilder = User
                        .withUsername(userProperty.username())
                        .password(encoder.encode(userProperty.password()))
                        .credentialsExpired(userProperty.credentialsExpired());
                if (authorities != null && !authorities.isEmpty()) {
                    userBuilder.authorities(authorities.stream().distinct().map(SimpleGrantedAuthority::new).toList());
                }
                if (roles != null && roles.length > 0) {
                    userBuilder.roles(roles);
                }
                final var user = userBuilder.build();
                if (!jdbcUserDetailsManager.userExists(user.getUsername())) jdbcUserDetailsManager.createUser(user);
            }
        }
        return jdbcUserDetailsManager;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowCredentials(true)
                .allowedMethods("*");
    }

    private UsernameLoginFilter tokenFilter(TokenGenerator tokenGenerator, TokenJpaRepository tokenJpaRepository) {
        final var provider = new AuthTokenProvider(tokenJpaRepository, tokenGenerator);

        final var manager = new ProviderManager(provider);

        final var authFilter = new UsernameLoginFilter(tokenGenerator);
        authFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        authFilter.setFilterProcessesUrl("/**");
        authFilter.setAuthenticationManager(manager);
        authFilter.setPostOnly(false);
        authFilter.setAuthenticationSuccessHandler(((request, response, authentication) -> {
        }));
        return authFilter;
    }

    private void sendError(HttpServletResponse response) throws IOException {
        final var httpStatusCode = 401;
        final var responseException = new ExceptionResponse(ErrorMessage.UNAUTHORIZED_ERROR, httpStatusCode);
        final var objectMapper = new ObjectMapper();
        final var json = objectMapper.writeValueAsString(responseException);
        response.setStatus(httpStatusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
