package team.hotpotato.domain.reaction.infrastructure.client;

public record NewsItemHttpResponse(
        String title,
        String summary,
        String link
) {}
