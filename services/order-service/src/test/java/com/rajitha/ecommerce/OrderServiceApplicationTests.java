package com.rajitha.ecommerce;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;


import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.config.import=",
		"application.config.customer-url=http://localhost:8090",
		"application.config.payment-url=http://localhost:8091",
		"application.config.product-url=http://localhost:8092"
})
@ExtendWith(MockitoExtension.class)
class OrderServiceApplicationTests {
	@MockitoBean
	private KafkaTemplate<String, Object> kafkaTemplate;

	@MockitoBean
	private KafkaAdmin kafkaAdmin;

	@Test
	void contextLoads() {
	}

}
