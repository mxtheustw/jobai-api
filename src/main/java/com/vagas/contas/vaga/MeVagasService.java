package com.vagas.contas.vaga;

import com.vagas.contas.erro.ApiException;
import com.vagas.contas.plano.PlanoService;
import com.vagas.contas.vaga.VagasDtos.FaltaItem;
import com.vagas.contas.vaga.VagasDtos.MatchDto;
import com.vagas.contas.vaga.VagasDtos.VagaComMatch;
import com.vagas.contas.vaga.VagasDtos.VagaDto;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * GET /me/vagas e GET /me/vagas/{id}: as vagas da pessoa com o resultado do match.
 * No plano Grátis, o "o que falta" vem vazio e oQueFaltaBloqueado = true.
 */
@Service
public class MeVagasService {

    private static final TypeReference<List<FaltaItem>> LISTA_FALTA = new TypeReference<>() {
    };

    private final VagaUsuarioRepository vagaUsuarioRepository;
    private final PlanoService planoService;
    private final JsonMapper json;

    public MeVagasService(VagaUsuarioRepository vagaUsuarioRepository, PlanoService planoService, JsonMapper json) {
        this.vagaUsuarioRepository = vagaUsuarioRepository;
        this.planoService = planoService;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<VagaComMatch> listar(UUID usuarioId, int limite) {
        int max = Math.max(1, Math.min(limite, 100));
        boolean pago = planoService.temPlanoPago(usuarioId);
        return vagaUsuarioRepository.feed(usuarioId, Limit.of(max)).stream()
                .map(linha -> montar((VagaUsuario) linha[0], (Vaga) linha[1], pago))
                .toList();
    }

    @Transactional(readOnly = true)
    public VagaComMatch buscar(UUID usuarioId, UUID vagaId) {
        List<Object[]> linhas = vagaUsuarioRepository.umaVaga(usuarioId, vagaId);
        if (linhas.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Vaga não encontrada no seu feed.");
        }
        Object[] linha = linhas.get(0);
        return montar((VagaUsuario) linha[0], (Vaga) linha[1], planoService.temPlanoPago(usuarioId));
    }

    private VagaComMatch montar(VagaUsuario vu, Vaga v, boolean pago) {
        Integer percentual = vu.getPercentual() == null ? null
                : vu.getPercentual().setScale(0, RoundingMode.HALF_UP).intValue();
        List<FaltaItem> falta = pago ? json.readValue(vu.getOQueFalta(), LISTA_FALTA) : List.of();
        MatchDto match = new MatchDto(percentual, vu.getPorQueCombina(), falta, !pago);
        return new VagaComMatch(VagaDto.de(v), match);
    }
}
