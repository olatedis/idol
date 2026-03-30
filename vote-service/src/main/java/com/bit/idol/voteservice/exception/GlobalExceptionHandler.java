package com.bit.idol.voteservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : "오류가 발생했습니다.";
        HttpStatus status = resolveStatus(message);
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private HttpStatus resolveStatus(String message) {
        if (message.contains("이미 투표에 참여하였습니다")) return HttpStatus.CONFLICT;
        if (message.contains("투표가 아직 시작되지 않았습니다")) return HttpStatus.BAD_REQUEST;
        if (message.contains("투표가 이미 종료되었습니다")) return HttpStatus.BAD_REQUEST;
        if (message.contains("이미 종료된 투표는 취소할 수 없습니다")) return HttpStatus.BAD_REQUEST;
        if (message.contains("투표 이력이 없습니다")) return HttpStatus.NOT_FOUND;
        if (message.contains("투표를 찾을 수 없습니다")) return HttpStatus.NOT_FOUND;
        if (message.contains("투표 기간 중 가입한 계정")) return HttpStatus.FORBIDDEN;
        if (message.contains("비정상적인 접근") || message.contains("비정상적인 투표")) return HttpStatus.TOO_MANY_REQUESTS;
        if (message.contains("잠시 후 다시 시도해주세요")) return HttpStatus.TOO_MANY_REQUESTS;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
