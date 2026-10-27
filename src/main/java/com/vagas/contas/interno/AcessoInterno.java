package com.vagas.contas.interno;

import com.vagas.contas.erro.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege as rotas /interno/** (usadas pelo coletor e pelo match, nunca pelo site).
 * Quem chama precisa mandar o cabeçalho:  X-Chave-Interna: <valor de INTERNO_CHAVE>
 * Se INTERNO_CHAVE não estiver configurada, as rotas internas ficam desligadas.
 */
@Component
public class AcessoInterno {

    public static final String CABECALHO = "X-Chave-Interna";

    private final byte[] chave;

    public AcessoInterno(@Value("${app.interno.chave:}") String chave) {
        this.chave = (chave == null || chave.isBlank()) ? null : chave.getBytes(StandardCharsets.UTF_8);
        if (this.chave != null && this.chave.length < 32) {
            throw new IllegalStateException("A variavel INTERNO_CHAVE precisa ter pelo menos 32 caracteres.");
        }
    }

    public void exigir(String recebida) {
        if (chave == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Rotas internas desligadas (INTERNO_CHAVE não configurada).");
        }
        // Comparação em tempo constante: não dá pistas de "quantas letras acertou"
        if (recebida == null || !MessageDigest.isEqual(chave, recebida.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Chave interna inválida.");
        }
    }
}
