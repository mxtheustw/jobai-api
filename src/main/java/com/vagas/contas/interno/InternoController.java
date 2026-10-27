package com.vagas.contas.interno;

import com.vagas.contas.perfil.PerfilService;
import com.vagas.contas.perfil.PerfilService.PaginaPerfis;
import com.vagas.contas.vaga.ColetaService;
import com.vagas.contas.vaga.ColetaService.ResultadoColeta;
import com.vagas.contas.vaga.ColetaService.ResultadoExpiracao;
import com.vagas.contas.vaga.MatchService;
import com.vagas.contas.vaga.VagaColetada;
import com.vagas.contas.vaga.VagasDtos.EnvioMatches;
import com.vagas.contas.vaga.VagasDtos.ResultadoMatches;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas internas: usadas pelo coletor (e, no futuro, pelo match). O site nunca chama.
 * Toda chamada precisa do cabeçalho X-Chave-Interna.
 */
@RestController
@RequestMapping("/interno")
public class InternoController {

    private final AcessoInterno acessoInterno;
    private final ColetaService coletaService;
    private final MatchService matchService;
    private final PerfilService perfilService;

    public InternoController(AcessoInterno acessoInterno, ColetaService coletaService,
                             MatchService matchService, PerfilService perfilService) {
        this.acessoInterno = acessoInterno;
        this.coletaService = coletaService;
        this.matchService = matchService;
        this.perfilService = perfilService;
    }

    /** { "vagas": [ { ...uma vaga... }, ... ] }  — até 500 por envio */
    public record EnvioVagas(List<VagaColetada> vagas) {
    }

    /** { "fonte": "gupy", "diasSemVer": 3 } */
    public record PedidoExpiracao(String fonte, Integer diasSemVer) {
    }

    @PostMapping("/vagas")
    public ResultadoColeta receberVagas(@RequestHeader(value = AcessoInterno.CABECALHO, required = false) String chave,
                                        @RequestBody EnvioVagas envio) {
        acessoInterno.exigir(chave);
        return coletaService.receber(envio.vagas());
    }

    /** { "matches": [ { usuarioId, fonte, id_fonte, percentual, porQueCombina, oQueFalta: [...] } ] } — até 1000 */
    @PostMapping("/matches")
    public ResultadoMatches receberMatches(@RequestHeader(value = AcessoInterno.CABECALHO, required = false) String chave,
                                           @RequestBody EnvioMatches envio) {
        acessoInterno.exigir(chave);
        return matchService.receber(envio.matches());
    }

    /** Perfis para o match, sem nome e e-mail. ?pagina=0&tamanho=100 (máx. 200). Continue enquanto temMais = true. */
    @GetMapping("/perfis")
    public PaginaPerfis perfis(@RequestHeader(value = AcessoInterno.CABECALHO, required = false) String chave,
                               @RequestParam(defaultValue = "0") int pagina,
                               @RequestParam(defaultValue = "100") int tamanho) {
        acessoInterno.exigir(chave);
        return perfilService.listarParaMatch(pagina, tamanho);
    }

    @PostMapping("/vagas/expirar")
    public ResultadoExpiracao expirar(@RequestHeader(value = AcessoInterno.CABECALHO, required = false) String chave,
                                      @RequestBody PedidoExpiracao pedido) {
        acessoInterno.exigir(chave);
        int dias = pedido.diasSemVer() == null ? 3 : pedido.diasSemVer();
        return coletaService.expirar(pedido.fonte(), dias);
    }
}
