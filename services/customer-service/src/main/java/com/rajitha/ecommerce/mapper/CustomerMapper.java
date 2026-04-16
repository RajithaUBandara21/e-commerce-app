package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import com.rajitha.ecommerce.dto.CustomerResponseDTO;

import com.rajitha.ecommerce.document.CustomerDocument;
import org.springframework.stereotype.Component;


@Component
public class CustomerMapper {

private final AddressMapper addressMapper;

CustomerMapper(AddressMapper addressMapper) {
    this.addressMapper = addressMapper;
}
    public CustomerDocument toCustomerEntity(CustomerRequestDTO customerDTO) {

        if (customerDTO == null) {
            return null;
        }
        return CustomerDocument.builder()
                .id(customerDTO.id())
                .firstName(customerDTO.firstName())
                .lastName(customerDTO.lastName())
                .email(customerDTO.email())
                .address(addressMapper.toAddressEntity(customerDTO.address()))
                .build();
    }

    public CustomerResponseDTO toCustomerResponseDTO(CustomerDocument customerDocument) {
    if (customerDocument == null) {return null;}
    return new CustomerResponseDTO(customerDocument.getId(), customerDocument.getFirstName(), customerDocument.getLastName(), customerDocument.getEmail(),addressMapper.toAddressDTO(customerDocument.getAddress()));
    }

}
