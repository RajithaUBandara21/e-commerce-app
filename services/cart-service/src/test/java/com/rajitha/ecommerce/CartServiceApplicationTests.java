package com.rajitha.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.config.import=",
		"application.config.order-url=http://localhost:8070"
})
class CartServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
