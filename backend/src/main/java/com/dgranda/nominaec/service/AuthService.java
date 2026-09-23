package com.dgranda.nominaec.service;

import com.dgranda.nominaec.dto.AuthDtos.LoginRequest;
import com.dgranda.nominaec.dto.AuthDtos.LoginResponse;
import com.dgranda.nominaec.entity.AppUser;
import com.dgranda.nominaec.repository.AppUserRepository;
import com.dgranda.nominaec.security.JwtService;
import com.dgranda.nominaec.security.LoginRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtService jwtService, LoginRateLimiter rateLimiter) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.dummyHash = encoder.encode("timing-equalizer-not-a-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientKey) {
        if (!rateLimiter.tryAcquire(clientKey)) {
            log.warn("Login rate limit exceeded");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espere un minuto.");
        }
        AppUser user = users.findByUsername(request.username()).filter(AppUser::isEnabled).orElse(null);
        // Always run BCrypt so that unknown users and wrong passwords take the same time.
        boolean matches = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) {
            throw new BadCredentialsException("invalid credentials");
        }
        rateLimiter.reset(clientKey);
        String token = jwtService.issue(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds(), user.getUsername(), user.getRole().name());
    }
}
