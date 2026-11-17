package dev.portfolio.web3.tx.api;

import dev.portfolio.web3.tx.application.IdempotencyConflictException;
import dev.portfolio.web3.tx.application.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionExceptionHandler {
    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ProblemDetail> conflict(IdempotencyConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(TransactionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }
}
