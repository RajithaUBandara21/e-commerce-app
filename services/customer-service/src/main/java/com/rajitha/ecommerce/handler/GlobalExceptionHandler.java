package com.rajitha.ecommerce.handler;

import com.rajitha.ecommerce.dto.ErrorResponseDTO;
import com.rajitha.ecommerce.exeption.CustomerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<String> handleCustomerNotFoundException(CustomerNotFoundException ex){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){

        var errors = new HashMap<String, String>();
       ex.getBindingResult().getAllErrors().forEach(error -> {
           var fieldName = ((FieldError) error).getField();
           var errorMessage = error.getDefaultMessage();
           errors.put(fieldName, errorMessage);
       });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(errors));
    }
}
