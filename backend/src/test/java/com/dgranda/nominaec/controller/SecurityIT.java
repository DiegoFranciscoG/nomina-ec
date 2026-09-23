package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.support.ApiClient;
import com.dgranda.nominaec.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends IntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ApiClient api;

    @Test
    void everyApiEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Autenticación requerida"));
        mvc.perform(get("/api/legal-parameters")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthAndDocsArePublicButActuatorOnlyExposesHealth() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void payrollRoleCannotChangeLegalParametersOrClosePeriods() throws Exception {
        String demo = api.token(ApiClient.DEMO, ApiClient.DEMO_PASSWORD);
        api.post(demo, "/api/legal-parameters", Map.of("code", "SBU", "value", 999, "validFrom", "2027-01-01",
                        "legalBasis", "x", "sourceUrl", "https://www.trabajo.gob.ec/", "reason", "Intento sin permisos"))
                .andExpect(status().isForbidden());
        api.post(demo, "/api/periods/999/close", null).andExpect(status().isForbidden());
        api.get(demo, "/api/legal-parameters").andExpect(status().isOk());
    }

    @Test
    void loginIsRateLimitedPerClient() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").with(r -> {
                        r.setRemoteAddr("203.0.113.7");
                        return r;
                    }).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));
        }
        mvc.perform(post("/api/auth/login").with(r -> {
                    r.setRemoteAddr("203.0.113.7");
                    return r;
                }).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unknownUserAndWrongPasswordLookTheSame() throws Exception {
        mvc.perform(post("/api/auth/login").with(r -> {
                    r.setRemoteAddr("203.0.113.9");
                    return r;
                }).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"nobody\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/employees").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(options("/api/employees").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
}
