package team.hotpotato.common.serde;

import org.apache.kafka.common.serialization.Serde;

public interface JsonSerde {
	<T> Serde<T> serde(Class<T> type);
}
