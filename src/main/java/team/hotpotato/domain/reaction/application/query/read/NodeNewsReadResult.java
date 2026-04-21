package team.hotpotato.domain.reaction.application.query.read;

import team.hotpotato.domain.reaction.domain.NewsItem;

import java.util.List;

public record NodeNewsReadResult(
        String nodeId,
        int count,
        List<NewsItem> news
) {}
