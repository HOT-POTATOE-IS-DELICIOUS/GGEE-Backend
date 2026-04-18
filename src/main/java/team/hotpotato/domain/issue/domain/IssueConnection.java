package team.hotpotato.domain.issue.domain;

public record IssueConnection(
        String sourceId,
        String targetId,
        double similarity
) {
}
