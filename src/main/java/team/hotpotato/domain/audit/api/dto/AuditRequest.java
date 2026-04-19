package team.hotpotato.domain.audit.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditRequest(
        @NotBlank(message = "검수할 입장문을 입력해주세요.")
        String text
) {
}
