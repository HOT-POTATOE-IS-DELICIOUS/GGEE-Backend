package team.hotpotato.infrastructure.snowflake;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("snowflake")
public record SnowflakeProperties(
        long workerId
) {
    private static final long MAX_WORKER_ID = 31L;

    public SnowflakeProperties {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw InvalidWorkerIdException.EXCEPTION;
        }
    }
}
