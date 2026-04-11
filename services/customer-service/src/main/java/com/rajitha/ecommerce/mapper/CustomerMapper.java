package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import com.rajitha.ecommerce.dto.CustomerResponseDTO;
import com.rajitha.ecommerce.entity.AddressEntity;
import com.rajitha.ecommerce.entity.CustomerEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class CustomerMapper {

private final AddressMapper addressMapper;

CustomerMapper(AddressMapper addressMapper) {
    this.addressMapper = addressMapper;
}
    public CustomerEntity toCustomerEntity(CustomerRequestDTO customerDTO) {

        if (customerDTO == null) {
            return null;
        }
        return CustomerEntity.builder()
                .id(customerDTO.id())
                .firstName(customerDTO.firstName())
                .lastName(customerDTO.lastName())
                .email(customerDTO.email())
                .address(addressMapper.toAddressEntity(customerDTO.address()))
                .build();
    }

    public CustomerResponseDTO toCustomerResponseDTO(CustomerEntity customerEntity) {
    if (customerEntity == null) {return null;}
    return new CustomerResponseDTO(customerEntity.getId(),customerEntity.getFirstName(),customerEntity.getLastName(),customerEntity.getEmail(),addressMapper.toAddressDTO(customerEntity.getAddress()));
    }

}
