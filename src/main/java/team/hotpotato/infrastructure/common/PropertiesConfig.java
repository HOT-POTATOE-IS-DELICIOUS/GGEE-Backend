package team.hotpotato.infrastructure.common;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackages = "team.hotpotato.infrastructure")
public class PropertiesConfig {
}
