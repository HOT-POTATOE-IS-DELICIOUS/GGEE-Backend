package team.hotpotato.domain.issue.application.query.read;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.issue.application.input.IssueGraphRead;
import team.hotpotato.domain.issue.application.output.IssueGraphSource;
import team.hotpotato.domain.issue.domain.IssueConnection;
import team.hotpotato.domain.issue.domain.IssueGraph;
import team.hotpotato.domain.issue.domain.IssueNode;
import team.hotpotato.domain.protect.application.input.GetProtectByUserId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueGraphReadQuery implements IssueGraphRead {
    private final IssueGraphSource issueGraphSource;
    private final GetProtectByUserId getProtectByUserId;

    @Override
    public Mono<IssueGraphReadResult> read(IssueGraphReadCommand command) {
        return getProtectByUserId.get(command.userId())
                .flatMap(protect -> issueGraphSource.read(protect.target(), protect.info()))
                .map(this::normalize);
    }

    private IssueGraphReadResult normalize(IssueGraph issueGraph) {
        List<IssueNode> sortedIssues = issueGraph.issues().stream()
                .sorted(Comparator.comparing(IssueNode::date, Comparator.nullsLast(String::compareTo)))
                .toList();

        Map<String, IssueNode> issueIndex = sortedIssues.stream()
                .collect(Collectors.toMap(IssueNode::id, Function.identity()));

        List<IssueConnection> normalizedConnections = issueGraph.connections().stream()
                .map(connection -> normalizeConnection(connection, issueIndex))
                .toList();

        return new IssueGraphReadResult(
                issueGraph.protectTarget(),
                sortedIssues,
                normalizedConnections
        );
    }

    private IssueConnection normalizeConnection(IssueConnection connection, Map<String, IssueNode> issueIndex) {
        IssueNode source = issueIndex.get(connection.sourceId());
        IssueNode target = issueIndex.get(connection.targetId());
        if (source == null || target == null) {
            return connection;
        }

        String sourceDate = source.date();
        String targetDate = target.date();
        if (sourceDate == null || targetDate == null || sourceDate.compareTo(targetDate) >= 0) {
            return connection;
        }

        return new IssueConnection(connection.targetId(), connection.sourceId(), connection.similarity());
    }
}
