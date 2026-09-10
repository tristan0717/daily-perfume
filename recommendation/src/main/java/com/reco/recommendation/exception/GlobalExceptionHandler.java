package com.reco.recommendation.exception;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ResponseEntity<Map<String,Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,Object>> missing(NoSuchElementException e) { return error(404, "향수를 찾을 수 없습니다."); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String,Object>> invalid(Exception e) { return error(400, "입력값을 확인해 주세요."); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> status(ResponseStatusException e) {
        var headers = new HttpHeaders();
        if (e.getStatusCode().value() == 429) headers.set("Retry-After", "60");
        return new ResponseEntity<>(Map.of("status", e.getStatusCode().value(), "message", Objects.toString(e.getReason(), "요청을 처리하지 못했습니다.")), headers, e.getStatusCode());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> unexpected(Exception e) {
        // Do not log external response bodies, request text or API credentials.
        log.error("Unhandled server exception: {}", e.getClass().getSimpleName());
        return error(500, "서버 내부 오류가 발생했습니다.");
    }
}
