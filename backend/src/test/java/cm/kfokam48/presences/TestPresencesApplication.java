package cm.kfokam48.presences;

import org.springframework.boot.SpringApplication;

public class TestPresencesApplication {

	public static void main(String[] args) {
		SpringApplication.from(PresencesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
