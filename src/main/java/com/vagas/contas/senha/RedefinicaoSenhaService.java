package com.vagas.contas.senha;

import com.vagas.contas.auth.SessaoRepository;
import com.vagas.contas.auth.TokenService;
import com.vagas.contas.email.EnvioEmail;
import com.vagas.contas.email.ModeloEmail;
import com.vagas.contas.erro.ApiException;
import com.vagas.contas.seguranca.LimiteTentativas;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * "Esqueci minha senha":
 * 1. a pessoa informa o e-mail → mandamos um link que vale por 1 hora;
 * 2. ela abre o link no site e cria uma senha nova.
 *
 * Por segurança, a resposta do passo 1 é SEMPRE a mesma, exista ou não a conta
 * (assim ninguém descobre quais e-mails estão cadastrados).
 */
@Service
public class RedefinicaoSenhaService {

    private static final long MINUTOS_VALIDADE = 60;

    private final UsuarioRepository usuarioRepository;
    private final TokenEmailRepository tokenEmailRepository;
    private final SessaoRepository sessaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final LimiteTentativas limiteTentativas;
    private final EnvioEmail envioEmail;
    private final String siteUrl;
    private final SecureRandom aleatorio = new SecureRandom();

    public RedefinicaoSenhaService(UsuarioRepository usuarioRepository,
                                   TokenEmailRepository tokenEmailRepository,
                                   SessaoRepository sessaoRepository,
                                   PasswordEncoder passwordEncoder,
                                   LimiteTentativas limiteTentativas,
                                   EnvioEmail envioEmail,
                                   @Value("${app.site.url:http://localhost:3000}") String siteUrl) {
        this.usuarioRepository = usuarioRepository;
        this.tokenEmailRepository = tokenEmailRepository;
        this.sessaoRepository = sessaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.limiteTentativas = limiteTentativas;
        this.envioEmail = envioEmail;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    /** POST /auth/esqueci-senha */
    @Transactional
    public void pedir(String emailInformado) {
        String email = emailInformado == null ? "" : emailInformado.trim().toLowerCase(Locale.ROOT);

        // Evita que alguém use a rota para encher a caixa de e-mail de outra pessoa
        String chave = "esqueci:" + email;
        limiteTentativas.verificar(chave);
        limiteTentativas.registrarFalha(chave);

        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(email).filter(Usuario::isAtivo);
        if (encontrado.isEmpty()) {
            return;  // mesma resposta de quando a conta existe
        }
        Usuario usuario = encontrado.get();

        if (!usuario.temSenha()) {
            envioEmail.enviar(usuario.getEmail(), usuario.getNome(), "Sua conta do JobAI entra com o Google",
                    ModeloEmail.html(usuario.getNome(), "Alguém pediu para redefinir a senha da sua conta do JobAI. "
                            + "Sua conta não tem senha: você entra com o botão <b>Entrar com Google</b>.", null, null,
                            "Se não foi você, ignore este e-mail."),
                    "Alguém pediu para redefinir a senha da sua conta do JobAI. Sua conta não tem senha: "
                            + "você entra com o botão Entrar com Google.");
            return;
        }

        Instant agora = Instant.now();
        tokenEmailRepository.invalidarAnteriores(usuario.getId(), TokenEmail.RESET_SENHA, agora);

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenEmailRepository.save(new TokenEmail(usuario.getId(), TokenService.hash(token), TokenEmail.RESET_SENHA,
                agora.plus(MINUTOS_VALIDADE, ChronoUnit.MINUTES)));

        String link = siteUrl + "/redefinir-senha?token=" + token;
        envioEmail.enviar(usuario.getEmail(), usuario.getNome(), "Redefinir sua senha do JobAI",
                ModeloEmail.html(usuario.getNome(), "Recebemos um pedido para redefinir a senha da sua conta. "
                        + "O link vale por 1 hora.", link, "Criar senha nova",
                        "Se não foi você, ignore este e-mail. Sua senha continua a mesma."),
                "Recebemos um pedido para redefinir a senha da sua conta do JobAI. "
                        + "Abra este link (vale por 1 hora): " + link
                        + "\n\nSe não foi você, ignore este e-mail. Sua senha continua a mesma.");
    }

    /** POST /auth/redefinir-senha */
    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenEmail t = (token == null ? Optional.<TokenEmail>empty()
                : tokenEmailRepository.findByTokenHash(TokenService.hash(token.trim())))
                .filter(x -> TokenEmail.RESET_SENHA.equals(x.getTipo()))
                .filter(TokenEmail::isValido)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Esse link é inválido ou já venceu. Peça um novo em \"Esqueci minha senha\"."));

        Usuario usuario = usuarioRepository.findById(t.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Conta não encontrada."));

        usuario.trocarSenha(passwordEncoder.encode(novaSenha));
        usuario.confirmarEmail();  // abriu o link que chegou no e-mail: o e-mail é mesmo dela
        t.marcarUsado();
        usuarioRepository.saveAndFlush(usuario);
        tokenEmailRepository.saveAndFlush(t);

        // Senha nova: desconecta todos os dispositivos e libera o login, se estava bloqueado
        sessaoRepository.revogarTodas(usuario.getId());
        limiteTentativas.limpar("login:" + usuario.getEmail().toLowerCase(Locale.ROOT));
    }
}
