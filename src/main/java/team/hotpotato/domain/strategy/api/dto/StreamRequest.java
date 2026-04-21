package team.hotpotato.domain.strategy.api.dto;

import jakarta.validation.constraints.NotBlank;

public record StreamRequest(
        @NotBlank String message
) {
}
