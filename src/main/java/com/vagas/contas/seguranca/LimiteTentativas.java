package com.vagas.contas.seguranca;

import com.vagas.contas.erro.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra quem tenta adivinhar senhas.
 *
 * Conta as tentativas ERRADAS por chave (ex.: "login:ana@email.com"). Passou do limite dentro da janela,
 * a chave fica bloqueada até as tentativas antigas "vencerem". Acertar a senha zera a contagem.
 *
 * Fica na memória da API: funciona porque hoje roda uma instância só. Se a API reiniciar, a contagem zera.
 */
@Component
public class LimiteTentativas {

    private final int maxFalhas;
    private final Duration janela;
    private final Map<String, Deque<Instant>> falhas = new ConcurrentHashMap<>();

    public LimiteTentativas(@Value("${app.seguranca.max-tentativas:5}") int maxFalhas,
                            @Value("${app.seguranca.janela-minutos:15}") long janelaMinutos) {
        this.maxFalhas = maxFalhas;
        this.janela = Duration.ofMinutes(janelaMinutos);
    }

    /** Lança 429 se a chave estourou o limite de tentativas erradas. */
    public void verificar(String chave) {
        Deque<Instant> lista = falhas.get(chave);
        if (lista == null) {
            return;
        }
        synchronized (lista) {
            limparAntigas(lista);
            if (lista.size() >= maxFalhas) {
                long minutos = Math.max(1, Duration.between(Instant.now(), lista.peekFirst().plus(janela)).toMinutes() + 1);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas erradas. Espere " + minutos + " minuto(s) e tente de novo.");
            }
        }
    }

    public void registrarFalha(String chave) {
        if (falhas.size() > 50_000) {
            faxina();
        }
        Deque<Instant> lista = falhas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (lista) {
            limparAntigas(lista);
            lista.addLast(Instant.now());
        }
    }

    /** Acertou: zera a contagem dessa chave. */
    public void limpar(String chave) {
        falhas.remove(chave);
    }

    private void limparAntigas(Deque<Instant> lista) {
        Instant limite = Instant.now().minus(janela);
        while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) {
            lista.pollFirst();
        }
    }

    /** Remove da memória as chaves sem tentativas recentes. */
    private void faxina() {
        falhas.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                limparAntigas(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }
}
