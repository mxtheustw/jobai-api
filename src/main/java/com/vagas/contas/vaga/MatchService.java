package com.vagas.contas.vaga;

import com.vagas.contas.erro.ApiException;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import com.vagas.contas.vaga.VagasDtos.ErroItem;
import com.vagas.contas.vaga.VagasDtos.FaltaItem;
import com.vagas.contas.vaga.VagasDtos.MatchRecebido;
import com.vagas.contas.vaga.VagasDtos.ResultadoMatches;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * POST /interno/matches: recebe o resultado do match (código do Igor) e grava em vagas_usuario.
 * Regra combinada: percentual + soma dos "impacto" do oQueFalta = 100.
 */
@Service
public class MatchService {

    public static final int MAXIMO_POR_ENVIO = 1000;
    /** Folga para arredondamento (ex.: 82,4 + 17,7 = 100,1). */
    private static final double FOLGA_SOMA = 1.0;

    private final VagaRepository vagaRepository;
    private final VagaUsuarioRepository vagaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final JsonMapper json;

    public MatchService(VagaRepository vagaRepository, VagaUsuarioRepository vagaUsuarioRepository,
                        UsuarioRepository usuarioRepository, JsonMapper json) {
        this.vagaRepository = vagaRepository;
        this.vagaUsuarioRepository = vagaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.json = json;
    }

    private record Chave(UUID usuarioId, UUID vagaId) {
    }

    @Transactional
    public ResultadoMatches receber(List<MatchRecebido> envio) {
        if (envio == null || envio.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Envie pelo menos um match em \"matches\".");
        }
        if (envio.size() > MAXIMO_POR_ENVIO) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Envie no máximo " + MAXIMO_POR_ENVIO + " matches por vez.");
        }

        List<ErroItem> recusados = new ArrayList<>();

        // 1. Encontra as vagas (por fonte + id_fonte) e os usuários citados, de uma vez
        Map<String, Set<String>> idsPorFonte = new HashMap<>();
        Set<UUID> usuarioIds = new HashSet<>();
        for (MatchRecebido m : envio) {
            if (m == null || m.fonte() == null || m.idFonte() == null || m.usuarioId() == null) continue;
            idsPorFonte.computeIfAbsent(m.fonte().trim().toLowerCase(Locale.ROOT), k -> new HashSet<>())
                    .add(m.idFonte().trim());
            usuarioIds.add(m.usuarioId());
        }
        Map<String, Vaga> vagas = new HashMap<>();
        idsPorFonte.forEach((fonte, ids) -> vagaRepository.findByFonteAndIdFonteIn(fonte, ids)
                .forEach(v -> vagas.put(fonte + "|" + v.getIdFonte(), v)));
        Set<UUID> usuariosExistentes = new HashSet<>();
        usuarioRepository.findAllById(usuarioIds).stream().map(Usuario::getId).forEach(usuariosExistentes::add);

        // 2. Valida cada match. Se o mesmo par (usuário, vaga) vier repetido, vale o último.
        Map<Chave, MatchRecebido> validos = new LinkedHashMap<>();
        for (int i = 0; i < envio.size(); i++) {
            MatchRecebido m = envio.get(i);
            String erro = validar(m, vagas, usuariosExistentes);
            if (erro != null) {
                recusados.add(new ErroItem(i, erro));
                continue;
            }
            Vaga vaga = vagas.get(m.fonte().trim().toLowerCase(Locale.ROOT) + "|" + m.idFonte().trim());
            validos.put(new Chave(m.usuarioId(), vaga.getId()), m);
        }

        // 3. Cria ou atualiza
        Map<Chave, VagaUsuario> existentes = new HashMap<>();
        if (!validos.isEmpty()) {
            Set<UUID> us = new HashSet<>();
            Set<UUID> vs = new HashSet<>();
            validos.keySet().forEach(k -> { us.add(k.usuarioId()); vs.add(k.vagaId()); });
            for (VagaUsuario vu : vagaUsuarioRepository.findByUsuarioIdInAndVagaIdIn(us, vs)) {
                existentes.put(new Chave(vu.getUsuarioId(), vu.getVagaId()), vu);
            }
        }

        int novos = 0;
        int atualizados = 0;
        List<VagaUsuario> paraSalvar = new ArrayList<>();
        for (Map.Entry<Chave, MatchRecebido> e : validos.entrySet()) {
            VagaUsuario vu = existentes.get(e.getKey());
            if (vu == null) {
                vu = new VagaUsuario(e.getKey().usuarioId(), e.getKey().vagaId());
                novos++;
            } else {
                atualizados++;
            }
            MatchRecebido m = e.getValue();
            vu.definirMatch(nota(m.percentual()), nota(m.notaFiltros()), nota(m.notaAfinidade()),
                    texto(m.porQueCombina()), json.writeValueAsString(limparFalta(m.oQueFalta())));
            paraSalvar.add(vu);
        }
        vagaUsuarioRepository.saveAll(paraSalvar);

        return new ResultadoMatches(envio.size(), novos, atualizados, recusados);
    }

    private static String validar(MatchRecebido m, Map<String, Vaga> vagas, Set<UUID> usuarios) {
        if (m == null) return "item vazio";
        if (m.usuarioId() == null) return "usuarioId vazio";
        if (!usuarios.contains(m.usuarioId())) return "usuário não existe";
        if (m.fonte() == null || m.idFonte() == null) return "fonte ou id_fonte vazios";
        if (!vagas.containsKey(m.fonte().trim().toLowerCase(Locale.ROOT) + "|" + m.idFonte().trim())) {
            return "vaga não existe (envie ela antes em /interno/vagas)";
        }
        if (m.percentual() == null || m.percentual() < 0 || m.percentual() > 100) return "percentual fora de 0 a 100";
        if (foraDe0a100(m.notaFiltros()) || foraDe0a100(m.notaAfinidade())) return "nota fora de 0 a 100";

        List<FaltaItem> falta = m.oQueFalta() == null ? List.of() : m.oQueFalta();
        double soma = 0;
        for (FaltaItem f : falta) {
            if (f == null || texto(f.item()) == null) return "item do oQueFalta sem texto";
            if (f.impacto() == null || f.impacto() < 0) return "impacto do oQueFalta inválido";
            soma += f.impacto();
        }
        // Regra do time: com itens no "o que falta", a conta precisa fechar em 100
        if (!falta.isEmpty() && Math.abs(m.percentual() + soma - 100) > FOLGA_SOMA) {
            return "percentual + soma dos impactos = " + Math.round(m.percentual() + soma) + " (precisa dar 100)";
        }
        return null;
    }

    private static List<FaltaItem> limparFalta(List<FaltaItem> lista) {
        if (lista == null) return List.of();
        return lista.stream()
                .map(f -> new FaltaItem(texto(f.item()),
                        texto(f.tipo()) == null ? "outro" : texto(f.tipo()).toLowerCase(Locale.ROOT),
                        f.impacto()))
                .toList();
    }

    private static boolean foraDe0a100(Double v) {
        return v != null && (v < 0 || v > 100);
    }

    private static BigDecimal nota(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static String texto(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
