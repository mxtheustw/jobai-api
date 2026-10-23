package com.vagas.contas.conta;

import com.vagas.contas.auth.AuthDtos.TokensResponse;
import com.vagas.contas.auth.AuthDtos.TrocarSenhaRequest;
import com.vagas.contas.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Rotas da conta da pessoa logada: trocar senha e excluir conta.
 * Precisam do cabeçalho "Authorization: Bearer <accessToken>".
 */
@RestController
@RequestMapping("/me")
public class ContaController {

    private final AuthService authService;
    private final ContaService contaService;

    public ContaController(AuthService authService, ContaService contaService) {
        this.authService = authService;
        this.contaService = contaService;
    }

    /** { "senha": "..." } — a senha é obrigatória para contas com senha. Contas Google podem mandar {}. */
    public record ExcluirContaRequest(String senha) {
    }

    /** Troca a senha. Encerra os outros dispositivos e devolve tokens novos para este. */
    @PutMapping("/senha")
    public TokensResponse trocarSenha(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TrocarSenhaRequest req) {
        return authService.trocarSenha(UUID.fromString(jwt.getSubject()), req);
    }

    /** Exclui a conta e todos os dados (perfil, currículo, sessões...). Não tem volta. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) ExcluirContaRequest req) {
        contaService.excluir(UUID.fromString(jwt.getSubject()), req == null ? null : req.senha());
    }
}
