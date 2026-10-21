package com.vagas.contas.curriculo;

import com.vagas.contas.perfil.PerfilService.CurriculoInfo;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Currículo da pessoa logada. Precisa do cabeçalho "Authorization: Bearer <accessToken>".
 */
@RestController
@RequestMapping("/me/curriculo")
public class CurriculoController {

    private final CurriculoService curriculoService;

    public CurriculoController(CurriculoService curriculoService) {
        this.curriculoService = curriculoService;
    }

    /** Envia (ou substitui) o currículo. Formato multipart, campo "arquivo", só PDF. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CurriculoInfo enviar(@AuthenticationPrincipal Jwt jwt, @RequestParam("arquivo") MultipartFile arquivo) {
        return curriculoService.enviar(UUID.fromString(jwt.getSubject()), arquivo);
    }

    /** Baixa o currículo enviado. */
    @GetMapping
    public ResponseEntity<InputStreamResource> baixar(@AuthenticationPrincipal Jwt jwt) {
        CurriculoService.Arquivo arquivo = curriculoService.baixar(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipo()))
                .contentLength(arquivo.tamanho())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(arquivo.conteudo()));
    }

    /** Remove o currículo. Não dá erro se não houver nenhum. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@AuthenticationPrincipal Jwt jwt) {
        curriculoService.apagar(UUID.fromString(jwt.getSubject()));
    }
}
