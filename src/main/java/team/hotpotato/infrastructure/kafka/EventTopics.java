package team.hotpotato.infrastructure.kafka;

public final class EventTopics {

    public static final String CRAWL_REQUEST = "crawl.request";
    public static final String CRAWL_JOB_CREATE = "crawl.job.create";
    public static final String CRAWL_RESULT = "crawl.result";
    public static final String CRAWL_COMMENT_DEDUPED = "crawl.comment.deduped";

    private EventTopics() {
    }
}
