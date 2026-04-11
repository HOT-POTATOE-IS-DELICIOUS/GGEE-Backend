package team.hotpotato.domain.reaction.application.output;

import reactor.core.publisher.Flux;

public interface CrawlJobCompletionEvents {

    Flux<String> completions();
}
