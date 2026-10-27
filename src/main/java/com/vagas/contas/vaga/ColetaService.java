package com.vagas.contas.vaga;

import com.vagas.contas.erro.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Recebe as vagas do coletor: cria as novas, atualiza as conhecidas e desativa as que sumiram. */
@Service
public class ColetaService {

    public static final int MAXIMO_POR_ENVIO = 500;
    private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private final VagaRepository vagaRepository;

    public ColetaService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    public record ErroItem(int indice, String motivo) {
    }

    public record ResultadoColeta(int recebidas, int novas, int atualizadas, List<ErroItem> recusadas) {
    }

    public record ResultadoExpiracao(int naoVistas, int prazoVencido) {
    }

    @Transactional
    public ResultadoColeta receber(List<VagaColetada> envio) {
        if (envio == null || envio.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Envie pelo menos uma vaga em \"vagas\".");
        }
        if (envio.size() > MAXIMO_POR_ENVIO) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Envie no máximo " + MAXIMO_POR_ENVIO + " vagas por vez.");
        }

        // 1. Limpa cada vaga. Recusadas não impedem as outras de serem salvas.
        //    Se a mesma vaga vier repetida no envio, vale a última.
        List<ErroItem> recusadas = new ArrayList<>();
        Map<String, Map<String, VagaColetada>> porFonte = new LinkedHashMap<>();
        for (int i = 0; i < envio.size(); i++) {
            VagaColetada bruta = envio.get(i);
            if (bruta == null) {
                recusadas.add(new ErroItem(i, "item vazio"));
                continue;
            }
            VagaColetada.Limpeza limpeza = bruta.limpar();
            if (limpeza.erro() != null) {
                recusadas.add(new ErroItem(i, limpeza.erro()));
                continue;
            }
            VagaColetada v = limpeza.vaga();
            porFonte.computeIfAbsent(v.fonte(), k -> new LinkedHashMap<>()).put(v.idFonte(), v);
        }

        // 2. Para cada fonte, busca de uma vez as vagas que já existem
        int novas = 0;
        int atualizadas = 0;
        List<Vaga> paraSalvar = new ArrayList<>();
        for (Map.Entry<String, Map<String, VagaColetada>> grupo : porFonte.entrySet()) {
            Map<String, Vaga> existentes = new HashMap<>();
            for (Vaga v : vagaRepository.findByFonteAndIdFonteIn(grupo.getKey(), grupo.getValue().keySet())) {
                existentes.put(v.getIdFonte(), v);
            }
            for (VagaColetada coletada : grupo.getValue().values()) {
                Vaga vaga = existentes.get(coletada.idFonte());
                if (vaga == null) {
                    vaga = new Vaga(coletada.fonte(), coletada.idFonte());
                    novas++;
                } else {
                    atualizadas++;
                }
                vaga.atualizarCom(coletada);
                paraSalvar.add(vaga);
            }
        }
        vagaRepository.saveAll(paraSalvar);

        return new ResultadoColeta(envio.size(), novas, atualizadas, recusadas);
    }

    /**
     * Desativa as vagas de uma fonte que o coletor não vê há "diasSemVer" dias,
     * e também todas as vagas com prazo de inscrição vencido.
     */
    @Transactional
    public ResultadoExpiracao expirar(String fonte, int diasSemVer) {
        if (fonte == null || fonte.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe a fonte.");
        }
        if (diasSemVer < 1 || diasSemVer > 90) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "diasSemVer precisa estar entre 1 e 90.");
        }
        Instant agora = Instant.now();
        Instant limite = agora.minus(diasSemVer, ChronoUnit.DAYS);
        int naoVistas = vagaRepository.desativarNaoVistas(fonte.trim().toLowerCase(Locale.ROOT), limite, agora);
        int vencidas = vagaRepository.desativarComPrazoVencido(LocalDate.now(BRASIL), agora);
        return new ResultadoExpiracao(naoVistas, vencidas);
    }
}
