package team.hotpotato.infrastructure.event.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import team.hotpotato.domain.reaction.application.output.CrawlJobCompletionEvents;

@Slf4j
@Component
public class CrawlJobCompletionSink implements CrawlJobCompletionEvents {

    private static final int BUFFER_SIZE = 256;

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(BUFFER_SIZE);

    public void complete(String jobId) {
        Sinks.EmitResult result = sink.tryEmitNext(jobId);
        if (result.isFailure()) {
            log.warn("크롤링 완료 이벤트 emit 실패. jobId={}, result={}", jobId, result);
        }
    }

    @Override
    public Flux<String> completions() {
        return sink.asFlux();
    }
}
