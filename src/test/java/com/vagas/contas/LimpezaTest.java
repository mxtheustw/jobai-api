package com.vagas.contas;

import com.vagas.contas.auth.Dispositivo;
import com.vagas.contas.auth.Sessao;
import com.vagas.contas.auth.SessaoRepository;
import com.vagas.contas.limpeza.LimpezaService;
import com.vagas.contas.senha.TokenEmail;
import com.vagas.contas.senha.TokenEmailRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A limpeza apaga só o que não serve mais e mantém o que ainda vale. */
class LimpezaTest extends TesteBase {

    @Autowired
    private LimpezaService limpezaService;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private TokenEmailRepository tokenEmailRepository;

    private static String hashNovo() {
        return "teste-" + UUID.randomUUID();
    }

    @Test
    void apagaVencidosEUsadosEMantemOsValidos() throws Exception {
        UUID usuarioId = UUID.fromString(ler(cadastrar(emailNovo()), "$.usuario.id"));
        Instant agora = Instant.now();

        String sessaoVencida = hashNovo();
        String sessaoEncerrada = hashNovo();
        String sessaoValida = hashNovo();
        sessaoRepository.save(new Sessao(usuarioId, sessaoVencida, Dispositivo.WEB, agora.minus(1, ChronoUnit.DAYS)));
        Sessao encerrada = new Sessao(usuarioId, sessaoEncerrada, Dispositivo.WEB, agora.plus(30, ChronoUnit.DAYS));
        encerrada.revogar();
        sessaoRepository.save(encerrada);
        sessaoRepository.save(new Sessao(usuarioId, sessaoValida, Dispositivo.WEB, agora.plus(30, ChronoUnit.DAYS)));

        String linkVencido = hashNovo();
        String linkUsado = hashNovo();
        String linkValido = hashNovo();
        tokenEmailRepository.save(new TokenEmail(usuarioId, linkVencido, TokenEmail.RESET_SENHA, agora.minus(1, ChronoUnit.HOURS)));
        TokenEmail usado = new TokenEmail(usuarioId, linkUsado, TokenEmail.RESET_SENHA, agora.plus(1, ChronoUnit.HOURS));
        usado.marcarUsado();
        tokenEmailRepository.save(usado);
        tokenEmailRepository.save(new TokenEmail(usuarioId, linkValido, TokenEmail.RESET_SENHA, agora.plus(1, ChronoUnit.HOURS)));

        LimpezaService.Resultado r = limpezaService.limpar();

        assertThat(r.sessoes()).isGreaterThanOrEqualTo(2);
        assertThat(r.links()).isGreaterThanOrEqualTo(2);
        assertThat(sessaoRepository.findByTokenHash(sessaoVencida)).isEmpty();
        assertThat(sessaoRepository.findByTokenHash(sessaoEncerrada)).isEmpty();
        assertThat(sessaoRepository.findByTokenHash(sessaoValida)).isPresent();
        assertThat(tokenEmailRepository.findByTokenHash(linkVencido)).isEmpty();
        assertThat(tokenEmailRepository.findByTokenHash(linkUsado)).isEmpty();
        assertThat(tokenEmailRepository.findByTokenHash(linkValido)).isPresent();
    }

    @Test
    void loginContinuaFuncionandoDepoisDaLimpeza() throws Exception {
        String email = emailNovo();
        String refresh = ler(cadastrar(email), "$.refreshToken");

        limpezaService.limpar();

        // a sessão do cadastro ainda vale
        postJson("/auth/refresh", java.util.Map.of("refreshToken", refresh))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
