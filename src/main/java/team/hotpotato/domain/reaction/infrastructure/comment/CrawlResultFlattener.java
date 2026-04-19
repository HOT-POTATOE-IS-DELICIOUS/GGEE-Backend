package team.hotpotato.domain.reaction.infrastructure.comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;
import team.hotpotato.infrastructure.crawler.message.CrawlCommentMessage;
import team.hotpotato.infrastructure.crawler.message.CrawlPostMessage;
import team.hotpotato.infrastructure.crawler.message.CrawlResultMessage;

@UtilityClass
public class CrawlResultFlattener {

    public static List<DeduplicatedCommentMessage> flattenComments(CrawlResultMessage crawlResult) {
        if (crawlResult == null || crawlResult.results() == null || crawlResult.results().isEmpty()) {
            return List.of();
        }

        long eventTimestampMs = parseTimestamp(crawlResult.timestamp());
        List<DeduplicatedCommentMessage> flattened = new ArrayList<>();

        for (CrawlPostMessage post : crawlResult.results()) {
            if (post == null || isBlank(post.url()) || post.comments() == null || post.comments().isEmpty()) {
                continue;
            }

            for (CrawlCommentMessage comment : post.comments()) {
                if (comment == null || comment.id() == null) {
                    continue;
                }

                flattened.add(new DeduplicatedCommentMessage(
                        buildDedupKey(comment.id(), post.url()),
                        crawlResult.site(),
                        crawlResult.keyword(),
                        crawlResult.timestamp(),
                        eventTimestampMs,
                        post.url().trim(),
                        post.title(),
                        comment.id(),
                        comment.parentId(),
                        comment.author(),
                        comment.date(),
                        comment.content(),
                        defaultString(comment.likes()),
                        defaultString(comment.dislikes())
                ));
            }
        }

        return flattened;
    }

    private static String buildDedupKey(Integer commentId, String url) {
        return commentId + "|" + url.trim();
    }

    private static long parseTimestamp(String timestamp) {
        if (isBlank(timestamp)) {
            return System.currentTimeMillis();
        }

        try {
            return Instant.parse(timestamp.trim()).toEpochMilli();
        } catch (Exception ignored) {
            return System.currentTimeMillis();
        }
    }

    private static String defaultString(String value) {
        return value == null ? "0" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
