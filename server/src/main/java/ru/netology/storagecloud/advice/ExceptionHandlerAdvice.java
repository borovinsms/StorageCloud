package ru.netology.storagecloud.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.netology.storagecloud.exceptions.BadCredentialsException;
import ru.netology.storagecloud.exceptions.InputDataException;
import ru.netology.storagecloud.exceptions.InternalServerException;
import ru.netology.storagecloud.models.errors.ExceptionResponse;

@RestControllerAdvice
public class ExceptionHandlerAdvice {

    @ExceptionHandler({BadCredentialsException.class, InputDataException.class})
    public ResponseEntity<ExceptionResponse> requestInputExceptionHandler(Exception e) {
        final var responseEntity = new ExceptionResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(responseEntity, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ExceptionResponse> internalServerExceptionHandler(InternalServerException e) {
        final var responseEntity = new ExceptionResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(responseEntity, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
