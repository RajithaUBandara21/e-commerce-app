package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.AddressDTO;
import com.rajitha.ecommerce.document.AddressDocument;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressDocument toAddressEntity(AddressDTO addressDTO) {
    if (addressDTO == null) {
        return null;
    }
    return  AddressDocument.builder()
                .street(addressDTO.street())
                .houseNumber(addressDTO.houseNumber())
                .zipCode(addressDTO.zipCode())
                .build();


    }

    public AddressDTO toAddressDTO(AddressDocument addressDocument) {
        if (addressDocument == null) {
            return null;
        }

        return new AddressDTO(addressDocument.getHouseNumber(), addressDocument.getZipCode(), addressDocument.getHouseNumber());

    }
}
