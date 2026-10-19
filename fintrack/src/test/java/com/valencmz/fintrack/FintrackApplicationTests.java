package com.valencmz.fintrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none" })
class FintrackApplicationTests {

	@Test
	void contextLoads() {
	}

}
