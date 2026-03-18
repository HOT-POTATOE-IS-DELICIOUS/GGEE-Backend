package team.hotpotato;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "team.hotpotato")
public class GgeeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GgeeBackendApplication.class, args);
    }

}
