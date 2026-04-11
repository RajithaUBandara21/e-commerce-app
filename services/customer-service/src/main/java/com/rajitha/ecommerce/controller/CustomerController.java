package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.CustomerRequestDTO;
import com.rajitha.ecommerce.dto.CustomerResponseDTO;
import com.rajitha.ecommerce.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<String> createCustomer(@RequestBody @Valid CustomerRequestDTO customerRequestDTO) {
                return ResponseEntity.ok(customerService.createCustomer(customerRequestDTO));
    }

    @PutMapping
    public ResponseEntity<?> updateCustomer(@RequestBody @Valid CustomerRequestDTO customerRequestDTO) {
    customerService.updateCustomer(customerRequestDTO);
    return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDTO>> getAll(){
        return ResponseEntity.ok(customerService.findAllCustomers());
    }

    @GetMapping("/exits/{customer-id}")
    public ResponseEntity<Boolean> existByCustomerId(@PathVariable("customer-id") String customerId){
        return ResponseEntity.ok(customerService.customerExistById(customerId));
    }

    @GetMapping("/{customer-id}")
    public ResponseEntity<CustomerResponseDTO> findByCustomerId(@PathVariable("customer-id") String customerId){
        return ResponseEntity.ok(customerService.customerFindById(customerId));
    }

    @DeleteMapping("/{customer-id}")
    public ResponseEntity<Void> deleteByCustomerId(@PathVariable("customer-id") String customerId){
        return ResponseEntity.accepted().build();
    }
}

