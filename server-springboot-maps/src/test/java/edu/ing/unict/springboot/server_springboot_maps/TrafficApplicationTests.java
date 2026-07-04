package edu.ing.unict.springboot.server_springboot_maps;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"google.maps.api.key=AIzaSyDmacxI26Ax65Qge3T1x9nmj44cdm_smNs"})
class TrafficApplicationTests {

	@Test
	void contextLoads() {
	}

}
