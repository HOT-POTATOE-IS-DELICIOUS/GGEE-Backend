package team.hotpotato.domain.issue.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.issue.api.dto.IssueConnectionResponse;
import team.hotpotato.domain.issue.api.dto.IssueNodeResponse;
import team.hotpotato.domain.issue.api.dto.IssuesResponse;
import team.hotpotato.domain.issue.application.input.IssueGraphRead;
import team.hotpotato.domain.issue.application.query.read.IssueGraphReadCommand;
import team.hotpotato.security.CustomAuthPrincipal;

@RequiredArgsConstructor
@RequestMapping("/issues")
@RestController
public class IssueController {
    private final IssueGraphRead issueGraphRead;

    @GetMapping
    public Mono<IssuesResponse> getIssues(@AuthenticationPrincipal CustomAuthPrincipal authPrincipal) {
        return issueGraphRead.read(new IssueGraphReadCommand(authPrincipal.userId()))
                .map(result -> new IssuesResponse(
                        result.protectTarget(),
                        result.issues().stream()
                                .map(issue -> new IssueNodeResponse(
                                        issue.id(),
                                        issue.title(),
                                        issue.summary(),
                                        issue.date(),
                                        issue.criticism(),
                                        issue.support(),
                                        issue.interest()
                                ))
                                .toList(),
                        result.connections().stream()
                                .map(connection -> new IssueConnectionResponse(
                                        connection.sourceId(),
                                        connection.targetId(),
                                        connection.similarity()
                                ))
                                .toList()
                ));
    }
}
