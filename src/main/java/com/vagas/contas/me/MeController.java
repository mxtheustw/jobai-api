package com.vagas.contas.me;

import com.vagas.contas.perfil.PerfilDto;
import com.vagas.contas.perfil.PerfilService;
import com.vagas.contas.perfil.PerfilService.MeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Rotas da pessoa logada. Precisam do cabeçalho:
 *   Authorization: Bearer <accessToken>
 */
@RestController
@RequestMapping("/me")
public class MeController {

    private final PerfilService perfilService;

    public MeController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /** Dados da conta + perfil completo, no formato do Profile do site. */
    @GetMapping
    public MeResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.buscar(UUID.fromString(jwt.getSubject()));
    }

    /** Atualiza o perfil inteiro (mande o Profile completo, igual ao do cadastro). */
    @PutMapping("/perfil")
    public MeResponse atualizarPerfil(@AuthenticationPrincipal Jwt jwt, @RequestBody PerfilDto perfil) {
        return perfilService.atualizar(UUID.fromString(jwt.getSubject()), perfil);
    }
}
