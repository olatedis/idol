package com.bit.idol.notifyservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    // 굳이 없어도 되는 파일이긴 합니다. 저도 뺄까 하다가 어짜피 만든거 오류잡기 편할거같고
    // 문제되면 빼려합니다.

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> notFound() {
        return ResponseEntity.status(404).body("NOT_FOUND");
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> forbidden(SecurityException e) {
        return ResponseEntity.status(403).body("FORBIDDEN");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("BAD_REQUEST");
    }
}
