package team.hotpotato.domain.reaction.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import team.hotpotato.domain.reaction.application.input.IndexingJobCompletionWaiter;

@RequiredArgsConstructor
@RequestMapping("/indexing/jobs")
@RestController
public class IndexingJobController {

    private final IndexingJobCompletionWaiter completionWaiter;

    @GetMapping(value = "/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamJobCompletion(@PathVariable String jobId) {
        return completionWaiter.waitForCompletion(jobId)
                .thenReturn(ServerSentEvent.<String>builder()
                        .event("completed")
                        .data("done")
                        .build())
                .flux();
    }
}
