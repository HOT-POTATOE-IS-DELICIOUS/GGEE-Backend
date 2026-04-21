package team.hotpotato.infrastructure.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration(proxyBeanMethods = false)
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean
    public JsonSerdeFactory jsonSerdeFactory(ObjectMapper objectMapper) {
        return new JsonSerdeFactory(objectMapper);
    }

    @Bean
    public SchemaRegistrySerdeFactory schemaRegistrySerdeFactory(
            @Value("${schema.registry.url}") String schemaRegistryUrl) {
        return new SchemaRegistrySerdeFactory(schemaRegistryUrl);
    }
}
