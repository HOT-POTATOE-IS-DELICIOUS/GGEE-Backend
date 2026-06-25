package team.hotpotato.domain.reaction.application.output;

import reactor.core.publisher.Mono;

public interface IndexingJobCompletionRepository {
    Mono<Boolean> isCompleted(String jobId);
}
