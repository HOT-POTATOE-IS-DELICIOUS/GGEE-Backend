package team.hotpotato.domain.reaction.application.usecase.wait;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionEvents;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionRepository;

import static org.assertj.core.api.Assertions.assertThat;

class IndexingJobCompletionWaiterUseCaseTest {

    @Test
    void completesImmediatelyWhenRepositoryAlreadyCompleted() {
        IndexingJobCompletionEvents events = () -> Flux.never();
        IndexingJobCompletionRepository repository = jobId -> Mono.just(true);
        IndexingJobCompletionWaiterUseCase useCase = new IndexingJobCompletionWaiterUseCase(events, repository);

        StepVerifier.create(useCase.waitForCompletion("123"))
                .verifyComplete();
    }

    @Test
    void completesWhenMatchingEventArrives() {
        Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();
        IndexingJobCompletionEvents events = sink::asFlux;
        IndexingJobCompletionRepository repository = jobId -> Mono.just(false);
        IndexingJobCompletionWaiterUseCase useCase = new IndexingJobCompletionWaiterUseCase(events, repository);

        StepVerifier.create(useCase.waitForCompletion("123"))
                .then(() -> assertThat(sink.tryEmitNext("123")).isEqualTo(Sinks.EmitResult.OK))
                .verifyComplete();
    }
}
