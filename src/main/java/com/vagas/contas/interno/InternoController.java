package com.vagas.contas.interno;

import com.vagas.contas.vaga.ColetaService;
import com.vagas.contas.vaga.ColetaService.ResultadoColeta;
import com.vagas.contas.vaga.ColetaService.ResultadoExpiracao;
import com.vagas.contas.vaga.VagaColetada;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public InternoController(AcessoInterno acessoInterno, ColetaService coletaService) {
        this.acessoInterno = acessoInterno;
        this.coletaService = coletaService;
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

    @PostMapping("/vagas/expirar")
    public ResultadoExpiracao expirar(@RequestHeader(value = AcessoInterno.CABECALHO, required = false) String chave,
                                      @RequestBody PedidoExpiracao pedido) {
        acessoInterno.exigir(chave);
        int dias = pedido.diasSemVer() == null ? 3 : pedido.diasSemVer();
        return coletaService.expirar(pedido.fonte(), dias);
    }
}
