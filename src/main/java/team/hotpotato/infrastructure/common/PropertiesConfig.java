package team.hotpotato.infrastructure.common;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackages = {
        "team.hotpotato.infrastructure",
        "team.hotpotato.domain.audit.infrastructure.client",
        "team.hotpotato.domain.issue.infrastructure"
})
public class PropertiesConfig {
}
