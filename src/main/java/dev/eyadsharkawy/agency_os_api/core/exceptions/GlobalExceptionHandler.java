package dev.eyadsharkawy.agency_os_api.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handelNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private ProblemDetail build(HttpStatus status, String detail) {
        ProblemDetail problemDetail =ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("message", detail);
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return problemDetail;
    }
}
