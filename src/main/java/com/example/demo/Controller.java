package com.example.demo;

import com.example.demo.ratelimit.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class Controller {

    @RateLimit
    @GetMapping("ping")
    public ResponseEntity<String> home(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body("pong");
    }
}
