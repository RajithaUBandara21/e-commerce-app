package com.rajitha.ecommerce.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Document
public class CustomerDocument {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private AddressDocument address;

}
