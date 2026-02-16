package team.hotpotato.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.infrastructure.adapter.snowflake.Snowflake;
import team.hotpotato.infrastructure.properties.SnowflakeProperties;

@Configuration
public class SnowflakeConfig {
    @Bean
    public Snowflake snowflake(SnowflakeProperties snowflakeProperties) {
        return new Snowflake(snowflakeProperties.workerId());
    }
}
