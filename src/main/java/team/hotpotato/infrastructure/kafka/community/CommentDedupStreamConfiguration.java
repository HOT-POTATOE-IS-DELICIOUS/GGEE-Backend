package team.hotpotato.infrastructure.kafka.community;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.domain.reaction.application.community.CrawlResultFlattener;
import team.hotpotato.domain.reaction.application.community.CrawlResultMessage;
import team.hotpotato.domain.reaction.application.community.DeduplicatedCommentMessage;
import team.hotpotato.infrastructure.kafka.JsonSerdeFactory;

@Configuration(proxyBeanMethods = false)
public class CommentDedupStreamConfiguration {

    private static final String COMPLETED = "completed";
    private static final String CRAWL_RESULT_TOPIC = "crawl.result";
    private static final String DEDUPLICATED_COMMENT_TOPIC = "crawl.comment.deduped";

    @Bean
    public KStream<String, DeduplicatedCommentMessage> commentDedupStream(
            StreamsBuilder streamsBuilder,
            CrawlerStreamsProperties properties,
            JsonSerdeFactory serdeFactory
    ) {
        streamsBuilder.addStateStore(dedupStoreBuilder(properties));

        KStream<String, DeduplicatedCommentMessage> stream = streamsBuilder
                .stream(CRAWL_RESULT_TOPIC, Consumed.with(Serdes.String(), serdeFactory.serde(CrawlResultMessage.class)))
                .filter((jobId, payload) -> payload != null && COMPLETED.equalsIgnoreCase(payload.status()))
                .flatMapValues(CrawlResultFlattener::flattenComments)
                .selectKey((jobId, comment) -> comment.id())
                .processValues(
                        new CommentDeduplicationProcessorSupplier(
                                properties.dedupStoreName(),
                                properties.dedupTtl(),
                                properties.cleanupInterval()
                        ),
                        properties.dedupStoreName()
                );

        stream.to(DEDUPLICATED_COMMENT_TOPIC, Produced.with(Serdes.String(), serdeFactory.serde(DeduplicatedCommentMessage.class)));

        return stream;
    }

    private StoreBuilder<KeyValueStore<String, Long>> dedupStoreBuilder(CrawlerStreamsProperties properties) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(properties.dedupStoreName()),
                Serdes.String(),
                Serdes.Long()
        );
    }
}
