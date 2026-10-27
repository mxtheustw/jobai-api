package com.vagas.contas.vaga;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Uma vaga do jeito que o coletor envia em POST /interno/vagas.
 * Aceita os nomes em camelCase (idFonte) ou como no CSV do coletor (id_fonte).
 */
public record VagaColetada(
        String fonte,
        @JsonAlias("id_fonte") String idFonte,
        String titulo,
        String empresa,
        String cidade,
        String uf,
        String modelo,
        String contrato,
        String senioridade,
        List<String> ferramentas,
        @JsonAlias("anos_experiencia") Integer anosExperiencia,
        @JsonAlias("publicada_em") LocalDate publicadaEm,
        @JsonAlias("prazo_inscricao") LocalDate prazoInscricao,
        String url,
        String descricao
) {

    private static final Set<String> MODELOS = Set.of("presencial", "hibrido", "remoto");
    private static final Set<String> SENIORIDADES =
            Set.of("estagio", "trainee", "junior", "pleno", "senior", "especialista");

    /** Resultado da limpeza: a vaga pronta para salvar, ou o motivo de ter sido recusada. */
    public record Limpeza(VagaColetada vaga, String erro) {
        static Limpeza ok(VagaColetada v) { return new Limpeza(v, null); }
        static Limpeza erro(String motivo) { return new Limpeza(null, motivo); }
    }

    /** Tira espaços, transforma texto vazio em "sem valor" e confere os campos obrigatórios. */
    public Limpeza limpar() {
        String f = minusculo(texto(fonte));
        String id = texto(idFonte);
        String t = texto(titulo);
        String u = texto(url);
        if (f == null) return Limpeza.erro("fonte vazia");
        if (id == null) return Limpeza.erro("id_fonte vazio");
        if (t == null) return Limpeza.erro("titulo vazio");
        if (u == null) return Limpeza.erro("url vazia");
        if (f.length() > 30 || id.length() > 100) return Limpeza.erro("fonte ou id_fonte longos demais");

        String m = minusculo(texto(modelo));
        if (m != null && !MODELOS.contains(m)) return Limpeza.erro("modelo inválido: " + m);

        String s = minusculo(texto(senioridade));
        if (s != null && !SENIORIDADES.contains(s)) return Limpeza.erro("senioridade inválida: " + s);

        if (anosExperiencia != null && (anosExperiencia < 0 || anosExperiencia > 60)) {
            return Limpeza.erro("anos_experiencia fora do esperado: " + anosExperiencia);
        }

        String ufLimpa = texto(uf);
        ufLimpa = (ufLimpa != null && ufLimpa.length() == 2) ? ufLimpa.toUpperCase(Locale.ROOT) : null;

        List<String> ferr = new ArrayList<>();
        if (ferramentas != null) {
            for (String item : ferramentas) {
                String x = texto(item);
                if (x != null && !ferr.contains(x)) ferr.add(x);
            }
        }

        return Limpeza.ok(new VagaColetada(
                f, id, cortar(t, 300), cortar(texto(empresa), 200), cortar(texto(cidade), 120), ufLimpa,
                m, cortar(minusculo(texto(contrato)), 30), s, ferr, anosExperiencia,
                publicadaEm, prazoInscricao, u, texto(descricao)));
    }

    private static String texto(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String minusculo(String v) {
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    private static String cortar(String v, int max) {
        return (v == null || v.length() <= max) ? v : v.substring(0, max);
    }
}
