package com.vagas.contas.curriculo;

import com.vagas.contas.aceite.AceiteService;
import com.vagas.contas.erro.ApiException;
import com.vagas.contas.perfil.Perfil;
import com.vagas.contas.perfil.PerfilRepository;
import com.vagas.contas.perfil.PerfilService.CurriculoInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Regras do currículo: um PDF por pessoa. Enviar outro substitui o anterior.
 */
@Service
public class CurriculoService {

    private static final Logger log = LoggerFactory.getLogger(CurriculoService.class);
    private static final String TIPO_PDF = "application/pdf";
    /** Todo PDF de verdade começa com estes bytes: "%PDF-" */
    private static final byte[] ASSINATURA_PDF = {'%', 'P', 'D', 'F', '-'};

    private final PerfilRepository perfilRepository;
    private final ArmazenamentoCurriculos armazenamento;
    private final AceiteService aceiteService;
    private final long tamanhoMaximoMb;

    public CurriculoService(PerfilRepository perfilRepository,
                            ArmazenamentoCurriculos armazenamento,
                            AceiteService aceiteService,
                            @Value("${app.curriculos.tamanho-maximo-mb:5}") long tamanhoMaximoMb) {
        this.perfilRepository = perfilRepository;
        this.armazenamento = armazenamento;
        this.aceiteService = aceiteService;
        this.tamanhoMaximoMb = tamanhoMaximoMb;
    }

    /** Arquivo pronto para ser baixado. */
    public record Arquivo(String nome, String tipo, long tamanho, InputStream conteudo) {
    }

    @Transactional
    public CurriculoInfo enviar(UUID usuarioId, MultipartFile arquivo) {
        aceiteService.exigirTermos(usuarioId);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escolha um arquivo para enviar.");
        }
        if (arquivo.getSize() > tamanhoMaximoMb * 1024 * 1024) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "O currículo pode ter até " + tamanhoMaximoMb + " MB.");
        }

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!pareceUmPdf(bytes)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Envie o currículo em PDF.");
        }

        Perfil perfil = perfilRepository.findById(usuarioId).orElseGet(() -> new Perfil(usuarioId));
        String chaveAntiga = perfil.getCurriculoChave();
        String chaveNova = usuarioId + "/" + UUID.randomUUID() + ".pdf";

        try {
            armazenamento.salvar(chaveNova, new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Depois que o banco confirmar: apaga o arquivo antigo.
        // Se o banco falhar: apaga o arquivo novo, para não sobrar lixo.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    apagarSemErro(chaveAntiga);
                } else {
                    apagarSemErro(chaveNova);
                }
            }
        });

        perfil.definirCurriculo(chaveNova, nomeSeguro(arquivo.getOriginalFilename()), TIPO_PDF,
                bytes.length, Instant.now());
        perfilRepository.saveAndFlush(perfil);
        return CurriculoInfo.de(perfil);
    }

    @Transactional(readOnly = true)
    public Arquivo baixar(UUID usuarioId) {
        Perfil perfil = perfilRepository.findById(usuarioId)
                .filter(Perfil::temCurriculo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Você ainda não enviou um currículo."));
        try {
            return new Arquivo(perfil.getCurriculoNome(), perfil.getCurriculoTipo(),
                    perfil.getCurriculoTamanho(), armazenamento.abrir(perfil.getCurriculoChave()));
        } catch (IOException e) {
            log.error("Currículo registrado no banco mas não encontrado: {}", perfil.getCurriculoChave(), e);
            throw new ApiException(HttpStatus.NOT_FOUND, "Não encontramos seu currículo. Envie de novo, por favor.");
        }
    }

    @Transactional
    public void apagar(UUID usuarioId) {
        perfilRepository.findById(usuarioId).filter(Perfil::temCurriculo).ifPresent(perfil -> {
            String chave = perfil.getCurriculoChave();
            perfil.removerCurriculo();
            perfilRepository.saveAndFlush(perfil);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    apagarSemErro(chave);
                }
            });
        });
    }

    private void apagarSemErro(String chave) {
        if (chave == null) {
            return;
        }
        try {
            armazenamento.apagar(chave);
        } catch (IOException | RuntimeException e) {
            log.warn("Não consegui apagar o arquivo {}", chave, e);
        }
    }

    private static boolean pareceUmPdf(byte[] bytes) {
        if (bytes.length < ASSINATURA_PDF.length) {
            return false;
        }
        for (int i = 0; i < ASSINATURA_PDF.length; i++) {
            if (bytes[i] != ASSINATURA_PDF[i]) {
                return false;
            }
        }
        return true;
    }

    /** Usa só o nome do arquivo (sem pastas), sem caracteres estranhos e com no máximo 255 letras. */
    private static String nomeSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "curriculo.pdf";
        }
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        nome = nome.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (nome.isEmpty()) {
            return "curriculo.pdf";
        }
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }
}
