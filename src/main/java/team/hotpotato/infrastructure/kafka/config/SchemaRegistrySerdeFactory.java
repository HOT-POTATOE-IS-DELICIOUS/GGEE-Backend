package team.hotpotato.infrastructure.kafka.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import team.hotpotato.common.serde.JsonSerde;

import java.util.Map;

public class SchemaRegistrySerdeFactory implements JsonSerde {

    private final String schemaRegistryUrl;

    public SchemaRegistrySerdeFactory(String schemaRegistryUrl) {
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    @Override
    public <T> Serde<T> serde(Class<T> type) {
        Map<String, Object> config = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl,
                KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, type.getName()
        );

        KafkaJsonSchemaSerializer<T> serializer = new KafkaJsonSchemaSerializer<>();
        serializer.configure(config, false);

        KafkaJsonSchemaDeserializer<T> deserializer = new KafkaJsonSchemaDeserializer<>();
        deserializer.configure(config, false);

        return Serdes.serdeFrom(serializer, deserializer);
    }
}
