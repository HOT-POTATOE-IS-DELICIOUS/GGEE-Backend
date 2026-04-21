package team.hotpotato.domain.reaction.api.dto;

import java.util.List;

public record NodeNewsResponse(
        String nodeId,
        int count,
        List<NewsItemResponse> news
) {}
