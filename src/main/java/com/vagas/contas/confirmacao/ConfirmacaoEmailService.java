package com.vagas.contas.confirmacao;

import com.vagas.contas.auth.TokenService;
import com.vagas.contas.email.EnvioEmail;
import com.vagas.contas.email.ModeloEmail;
import com.vagas.contas.erro.ApiException;
import com.vagas.contas.seguranca.LimiteTentativas;
import com.vagas.contas.senha.TokenEmail;
import com.vagas.contas.senha.TokenEmailRepository;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Confirmação de e-mail: depois do cadastro com e-mail e senha, mandamos um link.
 * Quando a pessoa abre o link, sabemos que o e-mail é mesmo dela.
 *
 * Quem entra pelo Google já chega confirmado (o Google confirmou o e-mail).
 * Por enquanto, quem não confirmou continua usando o site normalmente; o site só mostra um aviso.
 */
@Service
public class ConfirmacaoEmailService {

    private static final long HORAS_VALIDADE = 24;

    private final UsuarioRepository usuarioRepository;
    private final TokenEmailRepository tokenEmailRepository;
    private final LimiteTentativas limiteTentativas;
    private final EnvioEmail envioEmail;
    private final String siteUrl;
    private final SecureRandom aleatorio = new SecureRandom();

    public ConfirmacaoEmailService(UsuarioRepository usuarioRepository,
                                   TokenEmailRepository tokenEmailRepository,
                                   LimiteTentativas limiteTentativas,
                                   EnvioEmail envioEmail,
                                   @Value("${app.site.url:http://localhost:3000}") String siteUrl) {
        this.usuarioRepository = usuarioRepository;
        this.tokenEmailRepository = tokenEmailRepository;
        this.limiteTentativas = limiteTentativas;
        this.envioEmail = envioEmail;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    /** Manda o link de confirmação (chamado logo depois do cadastro). Só o último link vale. */
    @Transactional
    public void enviarLink(Usuario usuario) {
        if (usuario.isEmailVerificado()) {
            return;
        }
        Instant agora = Instant.now();
        tokenEmailRepository.invalidarAnteriores(usuario.getId(), TokenEmail.VERIFICACAO, agora);

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenEmailRepository.save(new TokenEmail(usuario.getId(), TokenService.hash(token), TokenEmail.VERIFICACAO,
                agora.plus(HORAS_VALIDADE, ChronoUnit.HOURS)));

        String link = siteUrl + "/confirmar-email?token=" + token;
        envioEmail.enviar(usuario.getEmail(), usuario.getNome(), "Confirme seu e-mail no JobAI",
                ModeloEmail.html(usuario.getNome(), "Falta só um passo: confirme que este e-mail é seu. "
                        + "O link vale por 24 horas.", link, "Confirmar meu e-mail",
                        "Se você não criou uma conta no JobAI, ignore este e-mail."),
                ModeloEmail.saudacao(usuario.getNome()) + "\n\nFalta só um passo: confirme que este e-mail é seu. "
                        + "Abra este link (vale por 24 horas): " + link
                        + "\n\nSe você não criou uma conta no JobAI, ignore este e-mail.\n\nEquipe JobAI");
    }

    /** POST /me/reenviar-confirmacao: manda um link novo para quem está logado. */
    @Transactional
    public void reenviar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre de novo."));
        if (usuario.isEmailVerificado()) {
            return;  // já confirmado: nada a fazer
        }
        // Evita encher a caixa de e-mail: no máximo 5 pedidos a cada 15 minutos
        String chave = "confirmar:" + usuarioId;
        limiteTentativas.verificar(chave);
        limiteTentativas.registrarFalha(chave);
        enviarLink(usuario);
    }

    /** POST /auth/confirmar-email: a pessoa abriu o link. */
    @Transactional
    public void confirmar(String token) {
        TokenEmail t = (token == null ? Optional.<TokenEmail>empty()
                : tokenEmailRepository.findByTokenHash(TokenService.hash(token.trim())))
                .filter(x -> TokenEmail.VERIFICACAO.equals(x.getTipo()))
                .filter(TokenEmail::isValido)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Esse link é inválido ou já venceu. Entre na sua conta e peça um novo."));

        Usuario usuario = usuarioRepository.findById(t.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Conta não encontrada."));

        usuario.confirmarEmail();
        t.marcarUsado();
        usuarioRepository.saveAndFlush(usuario);
        tokenEmailRepository.saveAndFlush(t);
    }
}
