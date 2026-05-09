package com.example.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Map<String, String> handle(Exception e) { return Map.of("error", "internal_error", "message", e.getMessage()); }
}
