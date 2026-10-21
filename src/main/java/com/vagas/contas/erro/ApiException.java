package com.vagas.contas.erro;

import org.springframework.http.HttpStatus;

/**
 * Erro "esperado", com uma mensagem amigável para mostrar na tela do site.
 * Ex.: e-mail já cadastrado, senha incorreta.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
