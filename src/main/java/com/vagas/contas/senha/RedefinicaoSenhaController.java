package com.vagas.contas.senha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** "Esqueci minha senha". Rotas públicas (a pessoa não está logada). */
@RestController
@RequestMapping("/auth")
public class RedefinicaoSenhaController {

    private final RedefinicaoSenhaService service;

    public RedefinicaoSenhaController(RedefinicaoSenhaService service) {
        this.service = service;
    }

    public record EsqueciSenhaRequest(@NotBlank(message = "Informe seu e-mail.") String email) {
    }

    public record RedefinirSenhaRequest(
            @NotBlank(message = "Link inválido.") String token,
            @NotBlank(message = "Crie uma nova senha.")
            @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.")
            String novaSenha
    ) {
    }

    /** Sempre responde a mesma coisa, exista ou não a conta. */
    @PostMapping("/esqueci-senha")
    public Map<String, String> esqueci(@Valid @RequestBody EsqueciSenhaRequest req) {
        service.pedir(req.email());
        return Map.of("mensagem", "Se existir uma conta com esse e-mail, enviamos um link para criar uma senha nova.");
    }

    @PostMapping("/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(@Valid @RequestBody RedefinirSenhaRequest req) {
        service.redefinir(req.token(), req.novaSenha());
    }
}
