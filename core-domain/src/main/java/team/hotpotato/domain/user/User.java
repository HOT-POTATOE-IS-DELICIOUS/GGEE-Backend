package team.hotpotato.domain.user;

import team.hotpotato.domain.user.exception.InvalidEmailFormatException;

public record User(
        Long userId,
        String email,
        String password,
        Role role
) {
    public User(Long userId, String email, String password, Role role) {
        this.userId = userId;
        validateEmail(email);
        this.email = email;
        this.password = password;
        this.role = role;
    }

    private void validateEmail(String email) {
        String pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if (!email.matches(pattern)) {
            throw InvalidEmailFormatException.EXCEPTION;
        }
    }
}
