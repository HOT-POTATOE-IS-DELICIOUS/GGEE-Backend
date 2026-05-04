package team.hotpotato.domain.member.domain;

public record User(
        Long id,
        String email,
        String password,
        Role role
) {
    public User {
        validateEmail(email);
    }

    private static void validateEmail(String email) {
        String pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if (!email.matches(pattern)) {
            throw InvalidEmailFormatException.EXCEPTION;
        }
    }
}
