package team.hotpotato.domain.reaction.application.usecase.wait;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.input.IndexingJobCompletionWaiter;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionEvents;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionRepository;

@Service
@RequiredArgsConstructor
public class IndexingJobCompletionWaiterUseCase implements IndexingJobCompletionWaiter {

    private static final Duration COMPLETION_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration COMPLETION_POLL_INTERVAL = Duration.ofSeconds(2);

    private final IndexingJobCompletionEvents completionEvents;
    private final IndexingJobCompletionRepository completionRepository;

    @Override
    public Mono<Void> waitForCompletion(String jobId) {
        return completionRepository.isCompleted(jobId)
                .flatMap(completed -> {
                    if (completed) {
                        return Mono.empty();
                    }

                    Flux<Boolean> eventSignal = completionEvents.completions()
                            .filter(jobId::equals)
                            .next()
                            .thenReturn(true)
                            .flux();

                    Flux<Boolean> pollingSignal = Flux.interval(COMPLETION_POLL_INTERVAL)
                            .flatMap(tick -> completionRepository.isCompleted(jobId))
                            .filter(Boolean.TRUE::equals)
                            .next()
                            .flux();

                    return Flux.merge(eventSignal, pollingSignal)
                            .next()
                            .timeout(COMPLETION_TIMEOUT)
                            .then();
                });
    }
}
