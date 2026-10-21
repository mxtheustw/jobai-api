package com.vagas.contas.curriculo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Guarda os currículos numa pasta do computador.
 * Padrão: "dados/curriculos" dentro da pasta da api-contas (ignorada pelo Git).
 * Para mudar, use a variável de ambiente CURRICULOS_PASTA.
 */
@Component
public class ArmazenamentoLocal implements ArmazenamentoCurriculos {

    private final Path base;

    public ArmazenamentoLocal(@Value("${app.curriculos.pasta:dados/curriculos}") String pasta) throws IOException {
        this.base = Paths.get(pasta).toAbsolutePath().normalize();
        Files.createDirectories(base);
    }

    @Override
    public void salvar(String chave, InputStream conteudo) throws IOException {
        Path destino = resolver(chave);
        Files.createDirectories(destino.getParent());
        Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream abrir(String chave) throws IOException {
        return Files.newInputStream(resolver(chave));
    }

    @Override
    public void apagar(String chave) throws IOException {
        Files.deleteIfExists(resolver(chave));
    }

    /** Impede que uma chave "esperta" (ex.: "../../") saia da pasta dos currículos. */
    private Path resolver(String chave) {
        Path caminho = base.resolve(chave).normalize();
        if (!caminho.startsWith(base)) {
            throw new IllegalArgumentException("Chave de arquivo inválida.");
        }
        return caminho;
    }
}
