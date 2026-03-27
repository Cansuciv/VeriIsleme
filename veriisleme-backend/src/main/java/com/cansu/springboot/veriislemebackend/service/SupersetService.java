package com.cansu.springboot.veriislemebackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupersetService {
    private final RestTemplate restTemplate = new RestTemplate(); //HTTP request atar
    private final ObjectMapper objectMapper = new ObjectMapper(); //JSON ↔ Java dönüşümü

    private final String supersetUrl = env("SUPERSET_URL", "http://localhost:8088");
    private final String username = env("SUPERSET_USERNAME", "admin");
    private final String password = env("SUPERSET_PASSWORD", "admin");

    public String getGuestToken(String dashboardId) throws Exception {
        //Verilen dashboard için embed token üretmek
        String accessToken = login();
        CsrfSession csrfSession = fetchCsrfSession(accessToken);

        Map<String, Object> payload = new HashMap<>();
        Map<String, String> resource = new HashMap<>();
        resource.put("type", "dashboard");
        resource.put("id", dashboardId);
        payload.put("resources", new Object[] { resource }); // hangi dashboard açılacak
        payload.put("rls", new Object[] {}); //row-level security (boş)
        Map<String, String> user = new HashMap<>();
        user.put("username", "embed_user");
        user.put("first_name", "Embed");
        user.put("last_name", "User");
        payload.put("user", user); //fake embed user

        String json = objectMapper.writeValueAsString(payload);
        String url = supersetUrl + "/api/v1/security/guest_token/"; //Request at
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.add("X-CSRFToken", csrfSession.csrfToken()); //güvenlik
        headers.add("Cookie", csrfSession.cookieHeader()); //session
        headers.add("Referer", supersetUrl + "/");
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Guest token yaniti bos.");
        }
        JsonNode node = objectMapper.readTree(body); //Response parse et
        return node.get("token").asText(); //tokenı alır
    }

    private String login() throws Exception {
        //Superset’e giriş yapar, JWT access token alır
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("provider", "db");
        payload.put("refresh", true);

        String json = objectMapper.writeValueAsString(payload);
        String url = supersetUrl + "/api/v1/security/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Login yaniti bos.");
        }
        JsonNode node = objectMapper.readTree(body);
        return node.get("access_token").asText();
    }

    private CsrfSession fetchCsrfSession(String accessToken) throws Exception {
        //CSRF token + cookie alır
        String url = supersetUrl + "/api/v1/security/csrf_token/";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("CSRF token yaniti bos.");
        }
        JsonNode node = objectMapper.readTree(body);
        JsonNode result = node.get("result");
        if (result == null || result.asText().isBlank()) {
            throw new IllegalStateException("CSRF token bulunamadi.");
        }
        String csrfToken = result.asText();
        String cookieHeader = buildCookieHeader(response);
        if (cookieHeader.isBlank()) {
            throw new IllegalStateException("CSRF session cookie bulunamadi.");
        }
        return new CsrfSession(csrfToken, cookieHeader);
    }

    private String buildCookieHeader(ResponseEntity<String> response) {
        if (response == null || response.getHeaders() == null) {
            return "";
        }
        List<String> cookies = response.getHeaders().get("Set-Cookie"); //Response’tan cookie’leri alır
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String cookie : cookies) {
            String part = cookie.split(";", 2)[0];
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private record CsrfSession(String csrfToken, String cookieHeader) {}

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

}
