package team.hotpotato.domain.reaction.application.input;

import reactor.core.publisher.Mono;
import team.hotpotato.domain.reaction.application.query.read.NodeNewsReadCommand;
import team.hotpotato.domain.reaction.application.query.read.NodeNewsReadResult;

public interface NodeNewsRead {
    Mono<NodeNewsReadResult> read(NodeNewsReadCommand command);
}
