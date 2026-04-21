package team.hotpotato.domain.strategy.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(
        @NotBlank(message = "메시지를 입력해주세요.")
        String message
) {
}
