package team.hotpotato.domain.reaction.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.hotpotato.domain.reaction.application.input.IndexingJobCompletionWaiter;
import team.hotpotato.domain.reaction.application.output.IndexingJobCompletionEvents;
import team.hotpotato.domain.reaction.application.usecase.wait.IndexingJobCompletionWaiterUseCase;

@Configuration(proxyBeanMethods = false)
public class ReactionUseCaseConfig {

    @Bean
    public IndexingJobCompletionWaiter indexingJobCompletionWaiter(IndexingJobCompletionEvents completionEvents) {
        return new IndexingJobCompletionWaiterUseCase(completionEvents);
    }
}
