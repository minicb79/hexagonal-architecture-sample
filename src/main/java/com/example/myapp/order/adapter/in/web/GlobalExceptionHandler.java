package com.example.myapp.order.adapter.in.web;

import com.example.myapp.order.domain.exception.CardNotFoundException;
import com.example.myapp.order.domain.exception.PaymentSessionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

/**
 * Global exception handler returning standard RFC 9457 Problem Detail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    public ProblemDetail handleCardNotFound(CardNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, 
                ex.getMessage()
        );
        problem.setTitle("Card Not Found");
        problem.setType(URI.create("https://api.example.com/errors/card-not-found"));
        return problem;
    }

    @ExceptionHandler(PaymentSessionFailedException.class)
    public ProblemDetail handlePaymentSessionFailed(PaymentSessionFailedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, 
                ex.getMessage()
        );
        problem.setTitle("Payment Gateway Failure");
        problem.setType(URI.create("https://api.example.com/errors/payment-gateway-failure"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                ex.getMessage()
        );
        problem.setTitle("Bad Request Parameters");
        problem.setType(URI.create("https://api.example.com/errors/bad-request-parameters"));
        return problem;
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, 
                "Validation failed for request parameters."
        );
        problem.setTitle("Bad Request Parameters");
        problem.setType(URI.create("https://api.example.com/errors/bad-request-parameters"));
        return problem;
    }
}
