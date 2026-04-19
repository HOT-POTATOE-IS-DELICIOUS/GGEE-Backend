package team.hotpotato.domain.audit.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.application.output.AuditSource;
import team.hotpotato.domain.audit.application.usecase.audit.AuditServiceUnavailableException;
import team.hotpotato.domain.audit.domain.AuditAnalysis;
import team.hotpotato.domain.audit.domain.AuditReview;
import team.hotpotato.domain.audit.domain.AuditSentence;
import team.hotpotato.domain.audit.domain.AuditSuggestion;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditHttpSource implements AuditSource {
    private final WebClient.Builder webClientBuilder;
    private final AuditAiProperties properties;

    @Override
    public Mono<AuditAnalysis> audit(String protectTarget, String protectTargetInfo, String text) {
        return webClientBuilder.baseUrl(properties.baseUrl()).build()
                .post()
                .uri("/audit")
                .bodyValue(new AuditHttpRequest(protectTarget, protectTargetInfo, text))
                .retrieve()
                .bodyToMono(AuditHttpResponse.class)
                .timeout(properties.timeout())
                .map(response -> new AuditAnalysis(
                        Optional.ofNullable(response.reviews()).orElse(List.of()).stream()
                                .map(review -> new AuditReview(
                                        new AuditSentence(
                                                review.sentence().sentenceText(),
                                                review.sentence().startOffset(),
                                                review.sentence().endOffset()
                                        ),
                                        Optional.ofNullable(review.perspectiveIds()).orElse(List.of()),
                                        Optional.ofNullable(review.perspectiveLabels()).orElse(List.of()),
                                        Optional.ofNullable(review.suggestions()).orElse(List.of()).stream()
                                                .map(suggestion -> new AuditSuggestion(
                                                        suggestion.startIndex(),
                                                        suggestion.endIndex(),
                                                        suggestion.before(),
                                                        suggestion.after(),
                                                        suggestion.reason()
                                                ))
                                                .toList()
                                ))
                                .toList()
                ))
                .doOnError(WebClientException.class, throwable -> log.warn("Audit API call failed", throwable))
                .doOnError(TimeoutException.class, throwable -> log.warn("Audit API call timed out"))
                .onErrorMap(WebClientException.class, throwable -> AuditServiceUnavailableException.EXCEPTION)
                .onErrorMap(TimeoutException.class, throwable -> AuditServiceUnavailableException.EXCEPTION);
    }
}
