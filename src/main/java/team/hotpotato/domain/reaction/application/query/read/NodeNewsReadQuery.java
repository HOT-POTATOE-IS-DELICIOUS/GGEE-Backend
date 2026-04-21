package team.hotpotato.domain.reaction.application.query.read;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.input.NodeNewsRead;
import team.hotpotato.domain.reaction.application.output.NodeNewsSource;

@Service
@RequiredArgsConstructor
public class NodeNewsReadQuery implements NodeNewsRead {
    private final NodeNewsSource nodeNewsSource;

    @Override
    public Mono<NodeNewsReadResult> read(NodeNewsReadCommand command) {
        return nodeNewsSource.read(command.nodeId())
                .map(nodeNews -> new NodeNewsReadResult(
                        nodeNews.nodeId(),
                        nodeNews.news().size(),
                        nodeNews.news()
                ));
    }
}
