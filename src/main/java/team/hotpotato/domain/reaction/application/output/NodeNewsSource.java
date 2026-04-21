package team.hotpotato.domain.reaction.application.output;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.domain.NodeNews;

public interface NodeNewsSource {
    Mono<NodeNews> read(String nodeId);
}
