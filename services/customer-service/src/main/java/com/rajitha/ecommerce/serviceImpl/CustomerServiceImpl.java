package com.rajitha.ecommerce.serviceImpl;

import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import com.rajitha.ecommerce.dto.CustomerResponseDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface CustomerServiceImpl {
    public String createCustomer(CustomerRequestDTO customerRequestDTO);

    void updateCustomer(@Valid CustomerRequestDTO customerRequestDTO);

    List<CustomerResponseDTO> findAllCustomers();

    Boolean customerExistById(String customerId);

    CustomerResponseDTO customerFindById(String customerId);

    void customerdeleteById(String customerId);
}
