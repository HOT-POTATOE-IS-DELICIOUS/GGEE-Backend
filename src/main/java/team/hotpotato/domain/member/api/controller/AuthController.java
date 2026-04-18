package team.hotpotato.domain.member.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import team.hotpotato.domain.member.api.dto.LoginRequest;
import team.hotpotato.domain.member.api.dto.LoginResponse;
import team.hotpotato.domain.member.api.dto.RefreshRequest;
import team.hotpotato.domain.member.api.dto.RefreshResponse;
import team.hotpotato.domain.member.api.dto.RegisterRequest;
import team.hotpotato.domain.member.api.dto.RegisterResponse;
import team.hotpotato.domain.member.application.input.UserLogin;
import team.hotpotato.domain.member.application.input.UserRegister;
import team.hotpotato.domain.member.application.input.UserLogout;
import team.hotpotato.domain.member.application.input.UserTokenRefresh;
import team.hotpotato.domain.member.application.usecase.login.LoginCommand;
import team.hotpotato.domain.member.application.usecase.logout.LogoutCommand;
import team.hotpotato.domain.member.application.usecase.refresh.RefreshCommand;
import team.hotpotato.domain.member.application.usecase.register.RegisterCommand;
import team.hotpotato.security.CustomAuthPrincipal;

@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {
    private final UserRegister userRegister;
    private final UserLogin userLogin;
    private final UserLogout userLogout;
    private final UserTokenRefresh userTokenRefresh;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return userRegister.register(
                        new RegisterCommand(
                                registerRequest.email(),
                                registerRequest.password(),
                                registerRequest.protectTarget(),
                                registerRequest.protectTargetInfo()
                        )
                )
                .map(result -> new RegisterResponse(result.indexingJobId(), result.accessToken(), result.refreshToken()));
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

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/refresh")
    public Mono<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        return userTokenRefresh.refresh(new RefreshCommand(refreshRequest.refreshToken()))
                .map(result -> new RefreshResponse(result.accessToken(), result.refreshToken()));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public Mono<Void> logout(@AuthenticationPrincipal CustomAuthPrincipal authPrincipal) {
        return userLogout.logout(new LogoutCommand(authPrincipal.userId()));
    }
}
