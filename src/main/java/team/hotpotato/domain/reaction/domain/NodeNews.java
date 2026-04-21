package team.hotpotato.domain.reaction.domain;

import java.util.List;

public record NodeNews(
        String nodeId,
        List<NewsItem> news
) {}
