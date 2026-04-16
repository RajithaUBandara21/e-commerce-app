package com.rajitha.ecommerce.document;

import lombok.*;
import org.springframework.validation.annotation.Validated;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Validated
public class AddressDocument {
    private String street;
    private String houseNumber;
    private String zipCode;
}
