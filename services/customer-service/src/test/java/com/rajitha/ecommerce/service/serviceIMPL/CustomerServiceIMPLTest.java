package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.document.AddressDocument;
import com.rajitha.ecommerce.document.CustomerDocument;
import com.rajitha.ecommerce.dto.AddressDTO;
import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import com.rajitha.ecommerce.mapper.AddressMapper;
import com.rajitha.ecommerce.mapper.CustomerMapper;
import com.rajitha.ecommerce.repository.CustomerRepository;
import com.rajitha.ecommerce.service.CustomerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
//@ExtendWith(MockitoExtension.class)
class CustomerServiceIMPLTest {

//    @InjectMocks
    CustomerServiceIMPL customerServiceIMPL;
//    @Mock
    CustomerMapper customerMapper;
//    @Mock
    AddressMapper addressMapper;

//    @Mock
    CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerMapper = Mockito.mock(CustomerMapper.class);
        addressMapper = Mockito.mock(AddressMapper.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        customerServiceIMPL = new CustomerServiceIMPL( customerRepository , customerMapper,addressMapper);
    }
    @Test
    public void shouldCreateCustomer() {
        CustomerRequestDTO customerRequestDTO = CustomerRequestDTO.builder()
                .id("1-id")
                .firstName("firstName")
                .lastName("lastName")
                .email("email")
                .address(AddressDTO.builder()
                        .street("street")
                        .houseNumber("123")
                        .zipCode("12356")
                        .build())
                .build();
        CustomerDocument customerDocument = CustomerDocument.builder()
                .id("1-id")
                .firstName("firstName")
                .lastName("lastName")
                .email("email")
                .address(
                        AddressDocument.builder()
                        .street("street")
                        .houseNumber("123")
                        .zipCode("12356")
                        .build()).build();


        Mockito.when(customerMapper.toCustomerEntity(customerRequestDTO)).thenReturn(customerDocument);
        Mockito.when(customerRepository.save(Mockito.any())).thenReturn(customerDocument);

        String customerId = customerServiceIMPL.createCustomer(customerRequestDTO);

        Assertions.assertNotNull(customerId);
        Assertions.assertEquals("1-id", customerId);
        Mockito.verify(customerMapper).toCustomerEntity(customerRequestDTO);
        Mockito.verify(customerRepository , Mockito.times(1)).save(Mockito.any());




    }



}