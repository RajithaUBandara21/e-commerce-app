package com.rajitha.ecommerce.handler;

import com.rajitha.ecommerce.dto.ErrorResponseDTO;
import com.rajitha.ecommerce.exeption.CategoryAccessDeniedException;
import com.rajitha.ecommerce.exeption.CategoryNotEmptyException;
import com.rajitha.ecommerce.exeption.CategoryNotFoundException;
import com.rajitha.ecommerce.exeption.ProductAccessDeniedException;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.exeption.ReviewAccessDeniedException;
import com.rajitha.ecommerce.exeption.ReviewAlreadyExistsException;
import com.rajitha.ecommerce.exeption.ReviewNotVerifiedPurchaseException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductPurchaseException.class)
    public ResponseEntity<String> handleCustomerNotFoundException(ProductPurchaseException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(ProductAccessDeniedException.class)
    public ResponseEntity<String> handleProductAccessDeniedException(ProductAccessDeniedException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<String> handleCategoryNotFoundException(CategoryNotFoundException ex){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(CategoryNotEmptyException.class)
    public ResponseEntity<String> handleCategoryNotEmptyException(CategoryNotEmptyException ex){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(CategoryAccessDeniedException.class)
    public ResponseEntity<String> handleCategoryAccessDeniedException(CategoryAccessDeniedException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    public ResponseEntity<String> handleReviewAccessDeniedException(ReviewAccessDeniedException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }

    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<String> handleReviewAlreadyExistsException(ReviewAlreadyExistsException ex){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(ReviewNotVerifiedPurchaseException.class)
    public ResponseEntity<String> handleReviewNotVerifiedPurchaseException(ReviewNotVerifiedPurchaseException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
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
