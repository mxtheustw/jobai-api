package com.vagas.contas.vaga;

import com.vagas.contas.vaga.VagasDtos.VagaComMatch;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Vagas da pessoa logada, com o match. Precisa do cabeçalho "Authorization: Bearer <accessToken>". */
@RestController
@RequestMapping("/me/vagas")
public class MeVagasController {

    private final MeVagasService meVagasService;

    public MeVagasController(MeVagasService meVagasService) {
        this.meVagasService = meVagasService;
    }

    /** Lista ordenada da maior porcentagem para a menor. ?limite= de 1 a 100 (padrão 50). */
    @GetMapping
    public List<VagaComMatch> listar(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(defaultValue = "50") int limite) {
        return meVagasService.listar(UUID.fromString(jwt.getSubject()), limite);
    }

    @GetMapping("/{vagaId}")
    public VagaComMatch buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID vagaId) {
        return meVagasService.buscar(UUID.fromString(jwt.getSubject()), vagaId);
    }
}
