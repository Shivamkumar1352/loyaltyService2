package com.loyaltyService.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=dGVzdGRGVzdGRGVzdGRGVzdGRGVzdGRGVzdGRGVzdA==",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
