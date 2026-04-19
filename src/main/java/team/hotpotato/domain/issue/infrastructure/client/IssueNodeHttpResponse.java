package team.hotpotato.domain.issue.infrastructure.client;

public record IssueNodeHttpResponse(
        String id,
        String title,
        String summary,
        String date,
        Double criticism,
        Double support,
        Double interest
) {
}
