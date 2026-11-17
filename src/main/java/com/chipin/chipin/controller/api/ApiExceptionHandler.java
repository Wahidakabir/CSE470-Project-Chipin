package com.chipin.chipin.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Turns the services' validation errors into JSON for the four API controllers
// in this package: {"error": "..."} with a 4xx status, not an HTML error page.
// It only applies to these controllers, so the rest of the app is unaffected.
@RestControllerAdvice(basePackages = "com.chipin.chipin.controller.api")
public class ApiExceptionHandler {

    // Bad input, e.g. a missing name or a negative amount.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", String.valueOf(e.getMessage())));
    }

    // The request is valid but not allowed right now, e.g. editing a finalized bill.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleNotAllowed(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
