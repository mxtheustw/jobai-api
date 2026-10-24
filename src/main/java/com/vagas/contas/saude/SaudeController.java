package com.vagas.contas.saude;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /saude — responde "ok" se a API está no ar.
 * Usado pela hospedagem para saber se a API subiu, e pelo site para "acordar"
 * a API no plano grátis (que dorme quando fica parada).
 */
@RestController
public class SaudeController {

    @GetMapping("/saude")
    public Map<String, String> saude() {
        return Map.of("status", "ok");
    }
}
