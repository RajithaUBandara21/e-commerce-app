package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.document.AddressDocument;
import com.rajitha.ecommerce.document.CustomerDocument;
import com.rajitha.ecommerce.dto.AddressDTO;
import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;

class CustomerMapperTest {

    private CustomerMapper customerMapper;
    private AddressMapper addressMapper;

    @BeforeEach
    void setUp() {
        addressMapper = new AddressMapper();
        customerMapper = new CustomerMapper(addressMapper);
    }

    @Test
    public void shouldMapToCustomerEntity() {
        CustomerRequestDTO customerRequestDTO = CustomerRequestDTO.builder()
                .id("id-1")
                .firstName("rajitha")
                .lastName("bandara")
                .email("rajithaubandara@gmail.com")
                .address(AddressDTO.builder()
                        .street("cross street")
                        .houseNumber("123")
                        .zipCode("1235")
                        .build())
                .build();

        CustomerDocument customerDocument = customerMapper.toCustomerEntity(customerRequestDTO);

        assertNotNull(customerDocument);
        assertEquals("rajitha", customerDocument.getFirstName());
        assertEquals("bandara", customerDocument.getLastName());
        assertEquals("rajithaubandara@gmail.com", customerDocument.getEmail());
        assertEquals("cross street", customerDocument.getAddress().getStreet());
    }


    @Test
    public void shouldMapToCustomerResponseDTO() {

        CustomerDocument customerDocument = CustomerDocument.builder()
                .id("id-1")
                .firstName("rajitha")
                .lastName("bandara")
                .email("rajithaubandara@gmail.com")
                .address(AddressDocument.builder()
                        .street("cross street")
                        .houseNumber("123")
                        .zipCode("1235")
                        .build())
                .build();

        var responseDTO = customerMapper.toCustomerResponseDTO(customerDocument);

        assertNotNull(responseDTO);
        assertEquals("id-1", responseDTO.id());
        assertEquals("rajitha", responseDTO.firstName());
        assertEquals("bandara", responseDTO.lastName());
        assertEquals("rajithaubandara@gmail.com", responseDTO.email());
        assertNotNull(responseDTO.address());
        assertEquals("cross street", responseDTO.address().street());
    }
}