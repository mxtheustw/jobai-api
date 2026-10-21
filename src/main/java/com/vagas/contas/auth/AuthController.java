package com.vagas.contas.auth;

import com.vagas.contas.auth.AuthDtos.CadastroRequest;
import com.vagas.contas.auth.AuthDtos.GoogleRequest;
import com.vagas.contas.auth.AuthDtos.LoginRequest;
import com.vagas.contas.auth.AuthDtos.RefreshRequest;
import com.vagas.contas.auth.AuthDtos.TokensResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * As "portas" de login da API. Todas são públicas (não precisam de token).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokensResponse cadastro(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrar(req);
    }

    @PostMapping("/login")
    public TokensResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.entrar(req);
    }

    @PostMapping("/refresh")
    public TokensResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.renovar(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        authService.sair(req.refreshToken());
    }

    @PostMapping("/google")
    public TokensResponse google(@Valid @RequestBody GoogleRequest req) {
        return authService.entrarComGoogle(req);
    }
}
