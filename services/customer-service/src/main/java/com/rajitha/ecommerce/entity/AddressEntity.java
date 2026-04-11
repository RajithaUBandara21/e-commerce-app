package com.rajitha.ecommerce.entity;

import lombok.*;
import org.springframework.validation.annotation.Validated;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Validated
public class AddressEntity {
    private String street;
    private String houseNumber;
    private String zipCode;
}
