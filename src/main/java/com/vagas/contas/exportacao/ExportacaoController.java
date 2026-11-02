package com.vagas.contas.exportacao;

import com.vagas.contas.exportacao.ExportacaoService.Exportacao;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** "Baixar meus dados": devolve um arquivo JSON com tudo o que guardamos sobre a pessoa logada. */
@RestController
@RequestMapping("/me")
public class ExportacaoController {

    private final ExportacaoService service;

    public ExportacaoController(ExportacaoService service) {
        this.service = service;
    }

    @GetMapping("/exportar")
    public ResponseEntity<Exportacao> exportar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meus-dados-jobai.json\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.exportar(UUID.fromString(jwt.getSubject())));
    }
}
