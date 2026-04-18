package team.hotpotato.infrastructure.kafka.community;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.domain.reaction.application.community.CrawlResultMessage;
import team.hotpotato.infrastructure.event.crawler.IndexingJobCompletionSink;
import team.hotpotato.infrastructure.kafka.EventTopics;
import team.hotpotato.infrastructure.kafka.JsonSerdeFactory;

@Configuration(proxyBeanMethods = false)
public class IndexingJobCompletionStreamConfiguration {

    private static final String ALL_DONE = "all_done";

    @Bean
    public KStream<String, CrawlResultMessage> indexingJobCompletionStream(
            StreamsBuilder streamsBuilder,
            JsonSerdeFactory serdeFactory,
            IndexingJobCompletionSink completionSink
    ) {
        return streamsBuilder
                .stream(EventTopics.CRAWL_RESULT, Consumed.with(Serdes.String(), serdeFactory.serde(CrawlResultMessage.class)))
                .filter((jobId, payload) -> payload != null && ALL_DONE.equalsIgnoreCase(payload.status()))
                .peek((jobId, payload) -> completionSink.complete(jobId));
    }
}
