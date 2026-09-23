package com.dgranda.nominaec.service;

import com.dgranda.nominaec.config.AppProperties;
import com.dgranda.nominaec.entity.AppUser;
import com.dgranda.nominaec.entity.UserRole;
import com.dgranda.nominaec.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the admin (and optional demo) user from environment variables on first start. */
@Service
@Order(1)
public class UserBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrapService.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties.Bootstrap config;

    public UserBootstrapService(AppUserRepository users, PasswordEncoder encoder, AppProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.config = properties.bootstrap();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing(config.adminUsername(), config.adminPassword(), UserRole.ADMIN);
        if (config.demoUsername() != null && !config.demoUsername().isBlank()
                && config.demoPassword() != null && !config.demoPassword().isBlank()) {
            createIfMissing(config.demoUsername(), config.demoPassword(), UserRole.PAYROLL);
        }
    }

    private void createIfMissing(String username, String password, UserRole role) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        users.save(user);
        log.info("Created {} user '{}'", role, username);
    }
}
