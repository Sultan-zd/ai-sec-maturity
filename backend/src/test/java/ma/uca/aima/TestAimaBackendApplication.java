package ma.uca.aima;

import org.springframework.boot.SpringApplication;

public class TestAimaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(AimaBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
