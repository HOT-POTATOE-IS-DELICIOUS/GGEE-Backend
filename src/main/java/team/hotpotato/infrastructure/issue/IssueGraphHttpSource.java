package team.hotpotato.infrastructure.issue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.issue.application.output.IssueGraphSource;
import team.hotpotato.domain.issue.application.usecase.read.IssueGraphServiceUnavailableException;
import team.hotpotato.domain.issue.domain.IssueConnection;
import team.hotpotato.domain.issue.domain.IssueGraph;
import team.hotpotato.domain.issue.domain.IssueNode;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Slf4j
public class IssueGraphHttpSource implements IssueGraphSource {
    private final WebClient webClient;
    private final IssueAiProperties properties;

    public IssueGraphHttpSource(WebClient.Builder webClientBuilder, IssueAiProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public Mono<IssueGraph> read(String protectTarget, String protectTargetInfo) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/issues")
                            .queryParam("entity_name", protectTarget);
                    if (protectTargetInfo != null && !protectTargetInfo.isBlank()) {
                        builder.queryParam("entity_info", protectTargetInfo);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(IssueGraphHttpResponse.class)
                .timeout(properties.timeout())
                .map(response -> new IssueGraph(
                        Optional.ofNullable(response.entityName()).orElse(protectTarget),
                        Optional.ofNullable(response.issues()).orElse(List.of()).stream()
                                .map(issue -> new IssueNode(
                                        issue.id(),
                                        issue.title(),
                                        issue.summary(),
                                        issue.date(),
                                        defaultToZero(issue.criticism()),
                                        defaultToZero(issue.support()),
                                        defaultToZero(issue.interest())
                                ))
                                .toList(),
                        Optional.ofNullable(response.connections()).orElse(List.of()).stream()
                                .map(connection -> new IssueConnection(
                                        connection.sourceId(),
                                        connection.targetId(),
                                        defaultToZero(connection.similarity())
                                ))
                                .toList()
                ))
                .doOnError(WebClientException.class, throwable -> log.warn("Issue graph API call failed", throwable))
                .doOnError(TimeoutException.class, throwable -> log.warn("Issue graph API call timed out"))
                .onErrorMap(WebClientException.class, throwable -> IssueGraphServiceUnavailableException.EXCEPTION)
                .onErrorMap(TimeoutException.class, throwable -> IssueGraphServiceUnavailableException.EXCEPTION);
    }

    private double defaultToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private record IssueGraphHttpResponse(
            @JsonProperty("entity_name") String entityName,
            List<IssueNodeHttpResponse> issues,
            List<IssueConnectionHttpResponse> connections
    ) {
    }

    private record IssueNodeHttpResponse(
            String id,
            String title,
            String summary,
            String date,
            Double criticism,
            Double support,
            Double interest
    ) {
    }

    private record IssueConnectionHttpResponse(
            @JsonProperty("source_id") String sourceId,
            @JsonProperty("target_id") String targetId,
            Double similarity
    ) {
    }
}
