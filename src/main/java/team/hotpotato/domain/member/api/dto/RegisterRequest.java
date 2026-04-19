package team.hotpotato.domain.member.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "이메일이 입력되지 않았습니다.")
        @Email(message = "잘못된 이메일 형식입니다.")
        String email,

        @Size(min = 8, max = 20, message = "비밀번호는 최소 8자 이상 20자 이하여야 합니다.")
        @NotBlank(message = "비밀번호가 입력되지 않았습니다.")
        String password,

        @NotBlank(message = "보호 대상을 입력해주세요.")
        String protectTarget,

        @NotBlank(message = "보호 대상 정보를 입력해주세요.")
        String protectTargetInfo
) {
}
