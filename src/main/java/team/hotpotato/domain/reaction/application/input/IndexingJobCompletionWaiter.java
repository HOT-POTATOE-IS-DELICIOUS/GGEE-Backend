package team.hotpotato.domain.reaction.application.input;

import reactor.core.publisher.Mono;

public interface IndexingJobCompletionWaiter {

    Mono<Void> waitForCompletion(String jobId);
}
