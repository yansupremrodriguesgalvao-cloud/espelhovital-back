package com.example.espelhovitalback.config;


import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Revise os campos informados.", "campos", fields));
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> malformed(Exception ex) {
        return error(400, "Verifique as datas e os parâmetros informados.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable(Exception ex) {
        return error(400, "Dados inválidos. Verifique os campos e as datas.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(Exception ex) {
        return error(403, "Acesso não permitido.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(Exception ex) {
        return error(409, "Os dados conflitam com um registro existente.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException ex) {
        return error(ex.getStatusCode().value(), ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex) {
        log.error("Falha interna: {}", ex.getClass().getSimpleName());
        return error(500, "Não foi possível concluir a operação.");
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "mensagem", message == null ? "Falha na operação." : message));
    }
}
