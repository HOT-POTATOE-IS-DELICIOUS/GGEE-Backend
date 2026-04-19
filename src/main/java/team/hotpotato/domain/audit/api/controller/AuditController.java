package team.hotpotato.domain.audit.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.audit.api.dto.AuditRequest;
import team.hotpotato.domain.audit.api.dto.AuditResponse;
import team.hotpotato.domain.audit.api.dto.AuditReviewResponse;
import team.hotpotato.domain.audit.api.dto.AuditSentenceResponse;
import team.hotpotato.domain.audit.api.dto.AuditSuggestionResponse;
import team.hotpotato.domain.audit.application.input.AuditStatement;
import team.hotpotato.domain.audit.application.usecase.audit.AuditCommand;
import team.hotpotato.security.CustomAuthPrincipal;

@RequiredArgsConstructor
@RequestMapping("/audit")
@RestController
public class AuditController {
    private final AuditStatement auditStatement;

    @PostMapping
    public Mono<AuditResponse> audit(
            @AuthenticationPrincipal CustomAuthPrincipal authPrincipal,
            @Valid @RequestBody AuditRequest auditRequest
    ) {
        return auditStatement.audit(new AuditCommand(
                        authPrincipal.userId(),
                        auditRequest.text()
                ))
                .map(result -> new AuditResponse(
                        result.auditId(),
                        result.messageId(),
                        result.reviews().stream()
                                .map(review -> new AuditReviewResponse(
                                        new AuditSentenceResponse(
                                                review.sentence().sentenceText(),
                                                review.sentence().startOffset(),
                                                review.sentence().endOffset()
                                        ),
                                        review.perspectiveIds(),
                                        review.perspectiveLabels(),
                                        review.suggestions().stream()
                                                .map(suggestion -> new AuditSuggestionResponse(
                                                        suggestion.startIndex(),
                                                        suggestion.endIndex(),
                                                        suggestion.before(),
                                                        suggestion.after(),
                                                        suggestion.reason()
                                                ))
                                                .toList()
                                ))
                                .toList()
                ));
    }
}
