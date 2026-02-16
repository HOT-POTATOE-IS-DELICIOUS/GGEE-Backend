package team.hotpotato.common.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ErrorCode {
    EXPIRED_TOKEN("만료된 JWT 토큰입니다."),
    INVALID_TOKEN("올바르지 않은 JWT 토큰입니다."),
    INVALID_TOKEN_TYPE("올바르지 않은 JWT 토큰 타입입니다."),
    EXPIRED_REFRESH_TOKEN("만료된 리프레시 토큰입니다."),

    EMAIL_ALREADY_EXISTS("이미 존재하는 이메일입니다."),
    INVALID_EMAIL_FORMAT("올바르지 않은 이메일 형식입니다."),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    INVALID_EMAIL_OR_PASSWORD("이메일 또는 비밀번호가 올바르지 않습니다."),

    METHOD_NOT_ALLOWED("잘못된 HTTP 메서드를 호출했습니다."),
    INTERNAL_SERVER_ERROR("서버 에러가 발생했습니다."),
    CLOCK_MOVED_BACKWARDS("시스템 시간에 에러가 발생하여 원래 시간보다 늦어졌습니다."),
    INVALID_WORKER_ID("snowflake.worker-id는 0~31 범위여야 합니다.");

    public final String message;
}
