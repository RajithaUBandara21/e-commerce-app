package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import com.rajitha.ecommerce.dto.CustomerResponseDTO;
import com.rajitha.ecommerce.entity.CustomerEntity;
import com.rajitha.ecommerce.exeption.CustomerNotFoundException;
import com.rajitha.ecommerce.mapper.AddressMapper;
import com.rajitha.ecommerce.repository.CustomerRepository;
import com.rajitha.ecommerce.mapper.CustomerMapper;
import com.rajitha.ecommerce.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CustomerServiceIMPL implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AddressMapper addressMapper;

    CustomerServiceIMPL(CustomerRepository customerRepository , CustomerMapper customerMapper, AddressMapper addressMapper ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.addressMapper = addressMapper;
    }


    public String createCustomer(CustomerRequestDTO customerRequestDTO) {
        var saveResponse = customerRepository.save(customerMapper.toCustomerEntity(customerRequestDTO));
        return saveResponse.getId() ;
    }


    public void updateCustomer(CustomerRequestDTO customerRequestDTO) {
        var customerEntity = customerRepository.findById(customerRequestDTO.id()).orElseThrow(() -> new CustomerNotFoundException("Cannot update customer :: Cannot found customer with id " + customerRequestDTO.id()));
        mergerCustomer(customerEntity , customerRequestDTO);
        customerRepository.save(customerEntity);
    }


    public List<CustomerResponseDTO> findAllCustomers() {
        List<CustomerResponseDTO> customerResponseDTO = customerRepository.findAll().stream().map(customerMapper :: toCustomerResponseDTO).collect(Collectors.toList());
        return customerResponseDTO;
    }


    public Boolean customerExistById(String customerId) {
        return customerRepository.existsById(customerId);

    }


    public CustomerResponseDTO customerFindById(String customerId) {

        return customerMapper.toCustomerResponseDTO(customerRepository.findById(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found with id " + customerId))) ;
    }


    public void customerdeleteById(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found for delete" + customerId);
        }
       customerRepository.deleteById(customerId);
    }

    private void mergerCustomer(CustomerEntity customerEntity , CustomerRequestDTO customerRequestDTO) {
        if(customerRequestDTO.firstName() != null && !customerRequestDTO.firstName().isBlank()){
            customerEntity.setFirstName(customerRequestDTO.firstName());
        };
        if(customerRequestDTO.lastName() != null && !customerRequestDTO.lastName().isBlank()){
            customerEntity.setLastName(customerRequestDTO.lastName());
        };
        if(customerRequestDTO.email() != null && !customerRequestDTO.email().isBlank()){
            customerEntity.setEmail(customerRequestDTO.email());
        };
        if(customerRequestDTO.address() != null ){
            customerEntity.setAddress(addressMapper.toAddressEntity(customerRequestDTO.address()));
        };
    }
}

