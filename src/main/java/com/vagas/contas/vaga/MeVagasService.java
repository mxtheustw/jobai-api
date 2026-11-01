package com.vagas.contas.vaga;

import com.vagas.contas.erro.ApiException;
import com.vagas.contas.plano.PlanoService;
import com.vagas.contas.vaga.VagasDtos.FaltaItem;
import com.vagas.contas.vaga.VagasDtos.MatchDto;
import com.vagas.contas.vaga.VagasDtos.VagaComMatch;
import com.vagas.contas.vaga.VagasDtos.VagaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * GET /me/vagas, /me/vagas/{id} e /me/vagas/outras.
 *
 * O Igor calcula o match de todas as vagas; a API decide o que cada plano vê:
 * - Grátis: no máximo N vagas novas por semana (as de maior %), e o "o que falta" bloqueado.
 * - Pago: todas as vagas e o "o que falta" completo.
 */
@Service
public class MeVagasService {

    private static final TypeReference<List<FaltaItem>> LISTA_FALTA = new TypeReference<>() {
    };
    private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private final VagaUsuarioRepository vagaUsuarioRepository;
    private final PlanoService planoService;
    private final JsonMapper json;
    private final int gratisPorSemana;
    private final int destaqueAPartirDe;

    public MeVagasService(VagaUsuarioRepository vagaUsuarioRepository, PlanoService planoService, JsonMapper json,
                          @Value("${app.planos.gratis-vagas-por-semana:3}") int gratisPorSemana,
                          @Value("${app.match.destaque-a-partir-de:80}") int destaqueAPartirDe) {
        this.vagaUsuarioRepository = vagaUsuarioRepository;
        this.planoService = planoService;
        this.json = json;
        this.gratisPorSemana = gratisPorSemana;
        this.destaqueAPartirDe = destaqueAPartirDe;
    }

    /** Feed com match. No Grátis, libera as vagas da semana (se ainda faltar) antes de listar. */
    @Transactional
    public List<VagaComMatch> listar(UUID usuarioId, int limite) {
        Limit max = Limit.of(Math.max(1, Math.min(limite, 100)));
        if (planoService.temPlanoPago(usuarioId)) {
            return montarLista(vagaUsuarioRepository.comMatch(usuarioId, max), true);
        }
        liberarVagasDaSemana(usuarioId);
        return montarLista(vagaUsuarioRepository.liberadas(usuarioId, max), false);
    }

    /** "Outras vagas que encontramos": sem porcentagem e sem "o que falta" (match = null). */
    @Transactional(readOnly = true)
    public List<VagaComMatch> outras(UUID usuarioId, int limite) {
        Limit max = Limit.of(Math.max(1, Math.min(limite, 100)));
        return vagaUsuarioRepository.semMatch(usuarioId, max).stream()
                .map(linha -> new VagaComMatch(VagaDto.de((Vaga) linha[1]), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public VagaComMatch buscar(UUID usuarioId, UUID vagaId) {
        List<Object[]> linhas = vagaUsuarioRepository.umaVaga(usuarioId, vagaId);
        if (linhas.isEmpty()) {
            throw naoEncontrada();
        }
        VagaUsuario vu = (VagaUsuario) linhas.get(0)[0];
        Vaga v = (Vaga) linhas.get(0)[1];
        if (vu.getPercentual() == null) {
            return new VagaComMatch(VagaDto.de(v), null);  // é uma das "outras vagas"
        }
        boolean pago = planoService.temPlanoPago(usuarioId);
        if (!pago && vu.getLiberadaEm() == null) {
            throw naoEncontrada();  // no Grátis, só dá para abrir as vagas já liberadas
        }
        return montar(vu, v, pago);
    }

    /** Libera, no Grátis, as vagas de maior % até completar o limite da semana (segunda a domingo). */
    private void liberarVagasDaSemana(UUID usuarioId) {
        Instant inicioDaSemana = LocalDate.now(BRASIL)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(BRASIL).toInstant();
        long jaLiberadas = vagaUsuarioRepository.liberadasDesde(usuarioId, inicioDaSemana);
        int faltam = gratisPorSemana - (int) jaLiberadas;
        if (faltam > 0) {
            List<VagaUsuario> novas = vagaUsuarioRepository.candidatasALiberar(usuarioId, Limit.of(faltam));
            novas.forEach(VagaUsuario::liberar);
            vagaUsuarioRepository.saveAllAndFlush(novas);
        }
    }

    private List<VagaComMatch> montarLista(List<Object[]> linhas, boolean pago) {
        return linhas.stream().map(l -> montar((VagaUsuario) l[0], (Vaga) l[1], pago)).toList();
    }

    private VagaComMatch montar(VagaUsuario vu, Vaga v, boolean pago) {
        Integer percentual = vu.getPercentual() == null ? null
                : vu.getPercentual().setScale(0, RoundingMode.HALF_UP).intValue();
        List<FaltaItem> falta = pago ? json.readValue(vu.getOQueFalta(), LISTA_FALTA) : List.of();
        boolean destaque = percentual != null && percentual >= destaqueAPartirDe;
        MatchDto match = new MatchDto(percentual, vu.getPorQueCombina(), falta, !pago, destaque);
        return new VagaComMatch(VagaDto.de(v), match);
    }

    private static ApiException naoEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND, "Vaga não encontrada no seu feed.");
    }
}
