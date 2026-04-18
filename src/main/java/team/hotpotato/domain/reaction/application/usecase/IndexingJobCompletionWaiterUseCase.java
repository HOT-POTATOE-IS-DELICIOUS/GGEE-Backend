package team.hotpotato.domain.reaction.application.usecase;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.input.IndexingJobCompletionWaiter;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionEvents;

@RequiredArgsConstructor
public class IndexingJobCompletionWaiterUseCase implements IndexingJobCompletionWaiter {

    private static final Duration COMPLETION_TIMEOUT = Duration.ofMinutes(10);

    private final IndexingJobCompletionEvents completionEvents;

    @Override
    public Mono<Void> waitForCompletion(String jobId) {
        return completionEvents.completions()
                .filter(jobId::equals)
                .next()
                .timeout(COMPLETION_TIMEOUT)
                .then();
    }
}
