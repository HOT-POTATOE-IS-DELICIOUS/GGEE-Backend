package team.hotpotato.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import team.hotpotato.api.dto.request.LoginRequest;
import team.hotpotato.api.dto.request.RegisterRequest;
import team.hotpotato.api.dto.response.LoginResponse;
import team.hotpotato.application.dto.command.LoginCommand;
import team.hotpotato.application.dto.command.RegisterCommand;
import team.hotpotato.application.usecase.UserLogin;
import team.hotpotato.application.usecase.UserRegister;

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
