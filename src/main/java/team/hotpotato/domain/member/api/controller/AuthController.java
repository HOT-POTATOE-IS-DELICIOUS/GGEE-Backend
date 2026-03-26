package team.hotpotato.domain.member.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.api.dto.LoginRequest;
import team.hotpotato.domain.member.api.dto.LoginResponse;
import team.hotpotato.domain.member.api.dto.RegisterRequest;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.application.usecase.login.LoginCommand;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;

@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {
    private final UserRegister userRegister;
    private final UserLogin userLogin;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return userRegister.register(
                        new RegisterCommand(
                                registerRequest.email(),
                                registerRequest.password()
                        )
                )
                .then();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return userLogin.login(
                        new LoginCommand(
                                loginRequest.email(),
                                loginRequest.password()
                        )
                )
                .map(response -> new LoginResponse(
                                response.accessToken(), response.refreshToken()
                        )
                );
    }
}
