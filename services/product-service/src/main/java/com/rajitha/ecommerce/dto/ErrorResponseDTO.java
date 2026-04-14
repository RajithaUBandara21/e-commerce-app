package com.rajitha.ecommerce.dto;

import java.util.Map;

public record ErrorResponseDTO (
        Map<String ,String> errors
){

}
