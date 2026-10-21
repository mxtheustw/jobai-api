package com.vagas.contas.auth;

import com.vagas.contas.erro.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

/** De onde a pessoa está entrando. Permite ficar logado no site e no app ao mesmo tempo. */
public enum Dispositivo {
    WEB, ANDROID, IOS;

    /** Converte o texto enviado pelo site/app. Se não vier nada, considera WEB. */
    public static Dispositivo de(String valor) {
        if (valor == null || valor.isBlank()) {
            return WEB;
        }
        try {
            return Dispositivo.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dispositivo inválido. Use WEB, ANDROID ou IOS.");
        }
    }
}
