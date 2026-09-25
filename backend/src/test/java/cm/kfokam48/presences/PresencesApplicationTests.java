package cm.kfokam48.presences;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PresencesApplicationTests {

	@Test
	void contextLoads() {
	}

}
