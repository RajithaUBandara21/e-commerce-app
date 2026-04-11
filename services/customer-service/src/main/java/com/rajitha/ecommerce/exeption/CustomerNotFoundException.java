package com.rajitha.ecommerce.exeption;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true) //to call super class of CustomerNotFoundException
@Data
public class CustomerNotFoundException extends RuntimeException {
private final String message;
}
