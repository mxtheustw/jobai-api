package com.vagas.contas.vaga;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Formatos JSON das vagas e do match (iguais ao frontend/src/types/api.ts). */
public final class VagasDtos {

    private VagasDtos() {
    }

    // ---------------- resposta para o site: { vaga, match } ----------------

    public record VagaDto(
            UUID id,
            String fonte,
            String titulo,
            String empresa,
            String cidade,
            String uf,
            String modelo,
            String contrato,
            String senioridade,
            List<String> ferramentas,
            Integer anosExperiencia,
            LocalDate publicadaEm,
            LocalDate prazoInscricao,
            String url,
            String descricao
    ) {
        static VagaDto de(Vaga v) {
            return new VagaDto(v.getId(), v.getFonte(), v.getTitulo(), v.getEmpresa(), v.getCidade(), v.getUf(),
                    v.getModelo(), v.getContrato(), v.getSenioridade(), v.getFerramentas(),
                    v.getAnosExperiencia() == null ? null : v.getAnosExperiencia().intValue(),
                    v.getPublicadaEm(), v.getPrazoInscricao(), v.getUrl(), v.getDescricao());
        }
    }

    public record FaltaItem(String item, String tipo, Integer impacto) {
    }

    public record MatchDto(
            Integer percentual,
            String porQueCombina,
            List<FaltaItem> oQueFalta,
            boolean oQueFaltaBloqueado,
            /** true quando o match é alto (a partir de 80%): ganha o selo amarelo no site. */
            boolean destaque
    ) {
    }

    /** match vem null nas "outras vagas que encontramos" (sem porcentagem). */
    public record VagaComMatch(VagaDto vaga, MatchDto match) {
    }

    // ---------------- entrada do match: POST /interno/matches ----------------

    public record MatchRecebido(
            UUID usuarioId,
            String fonte,
            @JsonAlias("id_fonte") String idFonte,
            Double percentual,
            @JsonAlias("nota_filtros") Double notaFiltros,
            @JsonAlias("nota_afinidade") Double notaAfinidade,
            @JsonAlias("por_que_combina") String porQueCombina,
            @JsonAlias("o_que_falta") List<FaltaItem> oQueFalta
    ) {
    }

    public record EnvioMatches(List<MatchRecebido> matches) {
    }

    public record ErroItem(int indice, String motivo) {
    }

    public record ResultadoMatches(int recebidos, int novos, int atualizados, List<ErroItem> recusados) {
    }
}
