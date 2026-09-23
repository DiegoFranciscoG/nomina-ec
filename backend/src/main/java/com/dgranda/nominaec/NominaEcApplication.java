package com.dgranda.nominaec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Users authenticate through AuthService (BCrypt + JWT), so the default in-memory user is disabled. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class NominaEcApplication {

    public static void main(String[] args) {
        SpringApplication.run(NominaEcApplication.class, args);
    }
}
