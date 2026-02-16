package team.hotpotato.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackages = "team.hotpotato.infrastructure.properties")
public class PropertiesConfig {
}
