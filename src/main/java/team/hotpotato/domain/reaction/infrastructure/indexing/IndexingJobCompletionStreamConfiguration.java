package team.hotpotato.domain.reaction.infrastructure.indexing;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.infrastructure.crawler.CrawlerTopics;
import team.hotpotato.infrastructure.crawler.message.CrawlResultMessage;
import team.hotpotato.infrastructure.kafka.config.JsonSerdeFactory;

@Configuration(proxyBeanMethods = false)
public class IndexingJobCompletionStreamConfiguration {

    private static final String ALL_DONE = "all_done";

    @Bean
    public KStream<String, CrawlResultMessage> indexingJobCompletionStream(
            StreamsBuilder streamsBuilder,
            JsonSerdeFactory serdeFactory,
            InMemoryIndexingJobCompletionEvents completionEvents
    ) {
        return streamsBuilder
                .stream(CrawlerTopics.CRAWL_RESULT, Consumed.with(Serdes.String(), serdeFactory.serde(CrawlResultMessage.class)))
                .filter((jobId, payload) -> payload != null && ALL_DONE.equalsIgnoreCase(payload.status()))
                .peek((jobId, payload) -> completionEvents.complete(jobId));
    }
}
