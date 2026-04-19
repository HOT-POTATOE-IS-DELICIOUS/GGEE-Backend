package team.hotpotato.infrastructure.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import team.hotpotato.common.serde.JsonSerde;

@RequiredArgsConstructor
public class JsonSerdeFactory implements JsonSerde {

    private final ObjectMapper objectMapper;

    public <T> Serde<T> serde(Class<T> type) {
        return Serdes.serdeFrom(
                (topic, data) -> {
                    if (data == null) return null;
                    try {
                        return objectMapper.writeValueAsBytes(data);
                    } catch (Exception e) {
                        throw new RuntimeException("Json 직렬화에 실패했습니다.", e);
                    }
                },
                (topic, bytes) -> {
                    if (bytes == null) return null;
                    try {
                        return objectMapper.readValue(bytes, type);
                    } catch (Exception e) {
                        throw new RuntimeException("Json 역직렬화에 실패했습니다", e);
                    }
                }
        );
    }
}
