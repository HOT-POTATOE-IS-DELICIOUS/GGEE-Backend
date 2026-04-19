package team.hotpotato.infrastructure.crawler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CrawlerTopics {

    public static final String CRAWL_REQUEST = "crawl.request";
    public static final String CRAWL_RESULT = "crawl.result";
    public static final String CRAWL_COMMENT_DEDUPED = "crawl.comment.deduped";
}
