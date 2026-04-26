package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder

public record AddressDTO(
         String street,
         String houseNumber,
         String zipCode){
}
