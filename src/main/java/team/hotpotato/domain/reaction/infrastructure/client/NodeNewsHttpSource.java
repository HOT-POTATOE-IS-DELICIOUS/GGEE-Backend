package team.hotpotato.domain.reaction.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.output.NodeNewsSource;
import team.hotpotato.domain.reaction.application.query.read.NodeNewsServiceUnavailableException;
import team.hotpotato.domain.reaction.domain.NewsItem;
import team.hotpotato.domain.reaction.domain.NodeNews;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeNewsHttpSource implements NodeNewsSource {
    private final WebClient.Builder webClientBuilder;
    private final ReactionAiProperties properties;

    @Override
    public Mono<NodeNews> read(String nodeId) {
        return webClientBuilder.baseUrl(properties.baseUrl()).build()
                .get()
                .uri("/news/{nodeId}", nodeId)
                .retrieve()
                .bodyToMono(NodeNewsHttpResponse.class)
                .timeout(properties.timeout())
                .map(response -> new NodeNews(
                        response.nodeId(),
                        Optional.ofNullable(response.news()).orElse(List.of()).stream()
                                .map(item -> new NewsItem(item.title(), item.summary(), item.link()))
                                .toList()
                ))
                .doOnError(WebClientException.class, throwable -> log.warn("News API call failed", throwable))
                .doOnError(TimeoutException.class, throwable -> log.warn("News API call timed out"))
                .onErrorMap(WebClientException.class, throwable -> NodeNewsServiceUnavailableException.EXCEPTION)
                .onErrorMap(TimeoutException.class, throwable -> NodeNewsServiceUnavailableException.EXCEPTION);
    }
}
