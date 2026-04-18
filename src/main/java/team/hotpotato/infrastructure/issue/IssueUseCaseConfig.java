package team.hotpotato.infrastructure.issue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import team.hotpotato.domain.member.application.input.GetUser;
import team.hotpotato.domain.issue.application.input.IssueGraphRead;
import team.hotpotato.domain.issue.application.output.IssueGraphSource;
import team.hotpotato.domain.issue.application.usecase.read.IssueGraphReadUseCase;

@Configuration(proxyBeanMethods = false)
public class IssueUseCaseConfig {

    @Bean
    @ConditionalOnMissingBean(WebClient.Builder.class)
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public IssueGraphRead issueGraphRead(IssueGraphSource issueGraphSource, GetUser getUser) {
        return new IssueGraphReadUseCase(issueGraphSource, getUser);
    }

    @Bean
    public IssueGraphSource issueGraphSource(WebClient.Builder webClientBuilder, IssueAiProperties issueAiProperties) {
        return new IssueGraphHttpSource(webClientBuilder, issueAiProperties);
    }
}
