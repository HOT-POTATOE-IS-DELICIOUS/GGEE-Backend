package team.hotpotato.domain.reaction.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.api.dto.NewsItemResponse;
import team.hotpotato.domain.reaction.api.dto.NodeNewsResponse;
import team.hotpotato.domain.reaction.application.input.NodeNewsRead;
import team.hotpotato.domain.reaction.application.query.read.NodeNewsReadCommand;

@RequiredArgsConstructor
@RequestMapping("/news")
@RestController
public class NewsController {
    private final NodeNewsRead nodeNewsRead;

    @GetMapping("/{nodeId}")
    public Mono<NodeNewsResponse> getNews(@PathVariable String nodeId) {
        return nodeNewsRead.read(new NodeNewsReadCommand(nodeId))
                .map(result -> new NodeNewsResponse(
                        result.nodeId(),
                        result.count(),
                        result.news().stream()
                                .map(item -> new NewsItemResponse(
                                        item.title(),
                                        item.summary(),
                                        item.link()
                                ))
                                .toList()
                ));
    }
}
