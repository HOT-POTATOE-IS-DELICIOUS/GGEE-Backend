package team.hotpotato.domain.issue.application.dto;

import team.hotpotato.domain.issue.domain.IssueConnection;
import team.hotpotato.domain.issue.domain.IssueNode;

import java.util.List;

public record IssueGraphReadResult(
        String protectTarget,
        List<IssueNode> issues,
        List<IssueConnection> connections
) {
}
