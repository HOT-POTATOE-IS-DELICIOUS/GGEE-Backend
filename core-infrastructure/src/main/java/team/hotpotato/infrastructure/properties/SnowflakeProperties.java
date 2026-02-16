package team.hotpotato.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import team.hotpotato.infrastructure.adapter.snowflake.exception.InvalidWorkerIdException;

@ConfigurationProperties("snowflake")
public record SnowflakeProperties(long workerId) {
    private static final long MAX_WORKER_ID = 31L;

    public SnowflakeProperties {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw InvalidWorkerIdException.EXCEPTION;
        }
    }
}
