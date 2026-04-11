package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.AddressDTO;
import com.rajitha.ecommerce.entity.AddressEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressEntity toAddressEntity(AddressDTO addressDTO) {
    if (addressDTO == null) {
        return null;
    }
    return  AddressEntity.builder()
                .street(addressDTO.street())
                .houseNumber(addressDTO.houseNumber())
                .zipCode(addressDTO.zipCode())
                .build();


    }

    public AddressDTO toAddressDTO(AddressEntity addressEntity) {
        if (addressEntity == null) {
            return null;
        }

        return new AddressDTO(addressEntity.getHouseNumber(), addressEntity.getZipCode(), addressEntity.getHouseNumber());

    }
}
