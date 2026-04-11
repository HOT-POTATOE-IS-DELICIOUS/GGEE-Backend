package team.hotpotato.domain.reaction.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.reaction.application.input.CrawlJobCompletionWaiter;

@RequiredArgsConstructor
@RequestMapping("/crawl/jobs")
@RestController
public class CrawlJobController {

    private final CrawlJobCompletionWaiter completionWaiter;

    @GetMapping(value = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamJobCompletion(@PathVariable String jobId) {
        return completionWaiter.waitForCompletion(jobId)
                .thenReturn(ServerSentEvent.<String>builder()
                        .event("completed")
                        .data("done")
                        .build())
                .flux();
    }
}
