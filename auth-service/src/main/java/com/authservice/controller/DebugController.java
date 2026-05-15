package com.authservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DebugController {

    @Value("${GOOGLE_CLIENT_ID:NOT_FOUND}")
    private String googleId;

    @GetMapping("/api/v1/auth/debug-check")
    public Map<String, String> check() {
        Map<String, String> map = new HashMap<>();
        map.put("status", "Auth Service is ALIVE");
        map.put("googleIdPresent", String.valueOf(!googleId.equals("NOT_FOUND")));
        map.put("googleIdStart", googleId.length() > 5 ? googleId.substring(0, 5) : "TOO_SHORT");
        return map;
    }
}
