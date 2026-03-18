package team.hotpotato.infrastructure.snowflake;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.common.identity.IdGenerator;

@Configuration
public class SnowflakeConfig {
    @Bean
    public IdGenerator idGenerator(SnowflakeProperties snowflakeProperties) {
        return new Snowflake(snowflakeProperties.workerId());
    }
}
