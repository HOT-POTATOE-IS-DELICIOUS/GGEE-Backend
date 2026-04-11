package team.hotpotato.domain.reaction.application.usecase;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.input.CrawlJobCompletionWaiter;
import team.hotpotato.domain.reaction.application.output.CrawlJobCompletionEvents;

@Service
@RequiredArgsConstructor
public class CrawlJobCompletionWaiterUseCase implements CrawlJobCompletionWaiter {

    private static final Duration COMPLETION_TIMEOUT = Duration.ofMinutes(10);

    private final CrawlJobCompletionEvents completionEvents;

    @Override
    public Mono<Void> waitForCompletion(String jobId) {
        return completionEvents.completions()
                .filter(jobId::equals)
                .next()
                .timeout(COMPLETION_TIMEOUT)
                .then();
    }
}
