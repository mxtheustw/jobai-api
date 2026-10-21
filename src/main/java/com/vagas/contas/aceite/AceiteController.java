package com.vagas.contas.aceite;

import com.vagas.contas.aceite.AceiteService.AceiteResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * POST /me/aceites — registra uma declaração da pessoa logada.
 * Usado no cadastro pelo Google (TERMOS) e, no futuro, ao assinar plano pago (MAIORIDADE_ASSINATURA).
 */
@RestController
@RequestMapping("/me/aceites")
public class AceiteController {

    private final AceiteService aceiteService;

    public AceiteController(AceiteService aceiteService) {
        this.aceiteService = aceiteService;
    }

    public record AceiteRequest(
            @NotBlank(message = "Informe o tipo do aceite.") String tipo,
            @NotBlank(message = "Informe a versão dos termos.") String versao
    ) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AceiteResponse registrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AceiteRequest req) {
        return aceiteService.registrar(UUID.fromString(jwt.getSubject()), TipoAceite.de(req.tipo()), req.versao());
    }
}
