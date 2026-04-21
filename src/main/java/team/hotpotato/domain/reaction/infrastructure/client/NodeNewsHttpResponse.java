package team.hotpotato.domain.reaction.infrastructure.client;

import java.util.List;

public record NodeNewsHttpResponse(
        String nodeId,
        int count,
        List<NewsItemHttpResponse> news
) {}
