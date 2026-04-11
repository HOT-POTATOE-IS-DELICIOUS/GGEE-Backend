package team.hotpotato.infrastructure.kafka.community;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.domain.reaction.application.community.CrawlResultMessage;
import team.hotpotato.infrastructure.event.crawler.CrawlJobCompletionSink;
import team.hotpotato.infrastructure.kafka.JsonSerdeFactory;

@Configuration(proxyBeanMethods = false)
public class CrawlJobCompletionStreamConfiguration {

    private static final String ALL_DONE = "all_done";

    @Bean
    public KStream<String, CrawlResultMessage> crawlJobCompletionStream(
            StreamsBuilder streamsBuilder,
            CrawlerStreamsProperties properties,
            JsonSerdeFactory serdeFactory,
            CrawlJobCompletionSink completionSink
    ) {
        return streamsBuilder
                .stream(properties.resultEventTopic(), Consumed.with(Serdes.String(), serdeFactory.serde(CrawlResultMessage.class)))
                .filter((jobId, payload) -> payload != null && ALL_DONE.equalsIgnoreCase(payload.status()))
                .peek((jobId, payload) -> completionSink.complete(jobId));
    }
}
