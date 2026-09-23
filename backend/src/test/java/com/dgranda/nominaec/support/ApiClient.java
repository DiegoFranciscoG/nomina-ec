package com.dgranda.nominaec.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Thin MockMvc wrapper that logs in through the real endpoint and sends the Bearer token. */
@Component
public class ApiClient {

    public static final String ADMIN = "admin";
    public static final String ADMIN_PASSWORD = "integration-admin-pass";
    public static final String DEMO = "demo";
    public static final String DEMO_PASSWORD = "integration-demo-pass";

    private final MockMvc mvc;
    private final ObjectMapper json;

    public ApiClient(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    public String token(String username, String password) throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").with(r -> {
                            r.setRemoteAddr("10.0.0." + Math.abs(username.hashCode() % 200));
                            return r;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    public ResultActions get(String token, String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.get(url), token));
    }

    public ResultActions post(String token, String url, Object body) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.post(url), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "" : json.writeValueAsString(body)));
    }

    public ResultActions put(String token, String url, Object body) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.put(url), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    public ResultActions delete(String token, String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.delete(url), token));
    }

    public JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }
}
