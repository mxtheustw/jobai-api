package com.vagas.contas.aceite;

import com.vagas.contas.erro.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

/** Tipos de declaração que a pessoa faz. */
public enum TipoAceite {
    /** Cadastro: aceita os termos e declara "Tenho 14 anos ou mais". */
    TERMOS,
    /** Plano pago: "Tenho 18 anos ou mais, ou sou o responsável e autorizo a assinatura". */
    MAIORIDADE_ASSINATURA;

    public static TipoAceite de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe o tipo do aceite.");
        }
        try {
            return TipoAceite.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tipo de aceite inválido. Use TERMOS ou MAIORIDADE_ASSINATURA.");
        }
    }
}
