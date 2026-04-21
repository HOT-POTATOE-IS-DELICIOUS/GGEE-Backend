package team.hotpotato.domain.audit.infrastructure.client;

import java.util.List;

public record AuditReviewHttpResponse(
        AuditSentenceHttpResponse sentence,
        List<String> perspectiveIds,
        List<String> perspectiveLabels,
        List<AuditSuggestionHttpResponse> suggestions
) {
}
