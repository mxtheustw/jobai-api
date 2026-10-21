package com.vagas.contas.auth;

import com.vagas.contas.perfil.PerfilDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Formatos JSON que entram e saem das rotas /auth/... */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * POST /auth/cadastro
     * { "perfil": { ...o Profile do site... }, "senha": "...", "dispositivo": "WEB" }
     */
    public record CadastroRequest(
            @NotNull(message = "Envie os dados do perfil.")
            @Valid
            PerfilDto perfil,

            @NotBlank(message = "Crie uma senha.")
            @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.")
            String senha,

            /** Aceite dos termos + "Tenho 14 anos ou mais". Sem ele, a conta não é criada. */
            @NotNull(message = "Aceite os termos de uso para criar sua conta.")
            @Valid
            AceiteTermos aceiteTermos,

            String dispositivo
    ) {
    }

    /** { "versao": "2026-10" } */
    public record AceiteTermos(
            @NotBlank(message = "Informe a versão dos termos.") String versao
    ) {
    }

    /** POST /auth/login */
    public record LoginRequest(
            @NotBlank(message = "Informe seu e-mail.") String email,
            @NotBlank(message = "Informe sua senha.") String senha,
            String dispositivo
    ) {
    }

    /** POST /auth/refresh e POST /auth/logout */
    public record RefreshRequest(
            @NotBlank(message = "Envie o refreshToken.") String refreshToken
    ) {
    }

    /** POST /auth/google: o idToken é o que o botão "Entrar com Google" devolve no site. */
    public record GoogleRequest(
            @NotBlank(message = "Envie o idToken do Google.") String idToken,
            String dispositivo
    ) {
    }

    public record UsuarioResponse(UUID id, String nome, String email) {
    }

    /**
     * Resposta de cadastro, login, refresh e Google.
     * - accessToken: vai no cabeçalho "Authorization: Bearer ..." das outras rotas. Dura pouco.
     * - refreshToken: serve só para pegar um accessToken novo em /auth/refresh. Dura semanas.
     * - novoUsuario: true quando a conta acabou de ser criada (útil no Google para mandar ao cadastro).
     */
    public record TokensResponse(
            String accessToken,
            String refreshToken,
            String tipo,
            long expiraEmSegundos,
            UsuarioResponse usuario,
            boolean novoUsuario
    ) {
    }
}
