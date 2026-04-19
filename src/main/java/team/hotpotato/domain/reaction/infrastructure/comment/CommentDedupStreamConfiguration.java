package team.hotpotato.domain.reaction.infrastructure.comment;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.infrastructure.crawler.CrawlerTopics;
import team.hotpotato.infrastructure.crawler.message.CrawlResultMessage;
import team.hotpotato.infrastructure.kafka.config.JsonSerdeFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CommentDedupStreamProperties.class)
public class CommentDedupStreamConfiguration {

    private static final String COMPLETED = "completed";

    @Bean
    public KStream<String, DeduplicatedPostMessage> commentDedupStream(
            StreamsBuilder streamsBuilder,
            CommentDedupStreamProperties properties,
            JsonSerdeFactory serdeFactory
    ) {
        streamsBuilder.addStateStore(dedupStoreBuilder(properties));

        KStream<String, DeduplicatedPostMessage> stream = streamsBuilder
                .stream(CrawlerTopics.CRAWL_RESULT, Consumed.with(Serdes.String(), serdeFactory.serde(CrawlResultMessage.class)))
                .filter((jobId, payload) -> payload != null && COMPLETED.equalsIgnoreCase(payload.status()))
                .process(
                        new CommentDeduplicationProcessorSupplier(
                                properties.dedupStoreName(),
                                properties.dedupTtl(),
                                properties.cleanupInterval()
                        ),
                        properties.dedupStoreName()
                );

        stream.to(CrawlerTopics.CRAWL_COMMENT_DEDUPED, Produced.with(Serdes.String(), serdeFactory.serde(DeduplicatedPostMessage.class)));

        return stream;
    }

    private StoreBuilder<KeyValueStore<String, Long>> dedupStoreBuilder(CommentDedupStreamProperties properties) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(properties.dedupStoreName()),
                Serdes.String(),
                Serdes.Long()
        );
    }
}
