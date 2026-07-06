package com.ionista.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/secure")
    public Map<String, String> secureEndpoint() {
        return Map.of("message", "You accessed a protected endpoint successfully!");
    }
}