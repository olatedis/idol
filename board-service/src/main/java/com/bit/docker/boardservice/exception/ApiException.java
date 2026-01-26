package com.bit.docker.boardservice.exception;

import lombok.Getter;

/**
 * API 예외 공통
 */
@Getter
public class ApiException extends RuntimeException {
    private final int status;
    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
    //삭제할예정
}
