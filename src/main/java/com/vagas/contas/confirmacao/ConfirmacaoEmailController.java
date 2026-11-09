package com.vagas.contas.confirmacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Confirmação de e-mail. */
@RestController
public class ConfirmacaoEmailController {

    private final ConfirmacaoEmailService service;

    public ConfirmacaoEmailController(ConfirmacaoEmailService service) {
        this.service = service;
    }

    public record ConfirmarEmailRequest(@NotBlank(message = "Link inválido.") String token) {
    }

    /** Pública: a pessoa pode abrir o link do e-mail sem estar logada (ex.: no celular). */
    @PostMapping("/auth/confirmar-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@Valid @RequestBody ConfirmarEmailRequest req) {
        service.confirmar(req.token());
    }

    /** Precisa estar logado. Se o e-mail já estiver confirmado, não faz nada. */
    @PostMapping("/me/reenviar-confirmacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviar(@AuthenticationPrincipal Jwt jwt) {
        service.reenviar(UUID.fromString(jwt.getSubject()));
    }
}
