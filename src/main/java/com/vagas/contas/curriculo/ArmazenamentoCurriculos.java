package com.vagas.contas.curriculo;

import java.io.IOException;
import java.io.InputStream;

/**
 * Onde os arquivos de currículo ficam guardados.
 *
 * Hoje a implementação é uma pasta no computador (ArmazenamentoLocal).
 * Quando formos para o Supabase, basta criar outra classe que implemente
 * esta interface (ex.: ArmazenamentoSupabase) — o resto do código não muda.
 */
public interface ArmazenamentoCurriculos {

    void salvar(String chave, InputStream conteudo) throws IOException;

    InputStream abrir(String chave) throws IOException;

    void apagar(String chave) throws IOException;
}
