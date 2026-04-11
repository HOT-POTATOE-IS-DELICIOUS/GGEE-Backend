package team.hotpotato.domain.reaction.application.input;

import reactor.core.publisher.Mono;

public interface CrawlJobCompletionWaiter {

    Mono<Void> waitForCompletion(String jobId);
}
