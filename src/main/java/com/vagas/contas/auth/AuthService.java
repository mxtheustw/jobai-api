package com.vagas.contas.auth;

import com.vagas.contas.aceite.AceiteService;
import com.vagas.contas.aceite.TipoAceite;
import com.vagas.contas.auth.AuthDtos.CadastroRequest;
import com.vagas.contas.auth.AuthDtos.GoogleRequest;
import com.vagas.contas.auth.AuthDtos.LoginRequest;
import com.vagas.contas.auth.AuthDtos.TokensResponse;
import com.vagas.contas.auth.AuthDtos.TrocarSenhaRequest;
import com.vagas.contas.auth.AuthDtos.UsuarioResponse;
import com.vagas.contas.erro.ApiException;
import com.vagas.contas.perfil.PerfilDto;
import com.vagas.contas.perfil.PerfilService;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** As regras de cadastro, login, renovação, saída e Google. */
@Service
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";
    private static final String SESSAO_INVALIDA = "Sua sessão expirou. Entre de novo.";

    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository;
    private final PerfilService perfilService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AceiteService aceiteService;

    public AuthService(UsuarioRepository usuarioRepository,
                       SessaoRepository sessaoRepository,
                       PerfilService perfilService,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       GoogleTokenVerifier googleTokenVerifier,
                       AceiteService aceiteService) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository;
        this.perfilService = perfilService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifier = googleTokenVerifier;
        this.aceiteService = aceiteService;
    }

    /** Cria a conta + o perfil e já devolve a pessoa logada. */
    @Transactional
    public TokensResponse cadastrar(CadastroRequest req) {
        PerfilDto perfil = req.perfil();
        String email = normalizarEmail(perfil.email());

        // Confere a versão dos termos ANTES de criar qualquer coisa
        aceiteService.validarVersao(req.aceiteTermos().versao());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }

        Usuario usuario = Usuario.comSenha(perfil.nome().trim(), email, passwordEncoder.encode(req.senha()));
        usuarioRepository.saveAndFlush(usuario);
        aceiteService.registrar(usuario.getId(), TipoAceite.TERMOS, req.aceiteTermos().versao());
        perfilService.salvar(usuario.getId(), perfil);

        return responder(usuario, Dispositivo.de(req.dispositivo()), true);
    }

    @Transactional
    public TokensResponse entrar(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(req.email()))
                .filter(Usuario::isAtivo)
                .filter(u -> u.getSenhaHash() != null && passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS));

        return responder(usuario, Dispositivo.de(req.dispositivo()), false);
    }

    /** Troca um refreshToken válido por tokens novos (o antigo deixa de valer). */
    @Transactional
    public TokensResponse renovar(String refreshToken) {
        Sessao sessao = sessaoRepository.findByTokenHash(TokenService.hash(refreshToken))
                .filter(Sessao::isValida)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, SESSAO_INVALIDA));
        sessao.revogar();

        Usuario usuario = usuarioRepository.findById(sessao.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, SESSAO_INVALIDA));

        return responder(usuario, sessao.getDispositivo(), false);
    }

    /** Encerra a sessão daquele dispositivo. Não dá erro se ela já não existir. */
    @Transactional
    public void sair(String refreshToken) {
        sessaoRepository.findByTokenHash(TokenService.hash(refreshToken)).ifPresent(Sessao::revogar);
    }

    /**
     * Entrar com Google:
     * - se a conta já existe (pelo Google ou pelo mesmo e-mail), entra nela;
     * - se não existe, cria a conta com um perfil em branco e devolve novoUsuario = true,
     *   para o site levar a pessoa ao cadastro e depois salvar com PUT /me/perfil.
     */
    @Transactional
    public TokensResponse entrarComGoogle(GoogleRequest req) {
        GoogleTokenVerifier.ContaGoogle conta = googleTokenVerifier.verificar(req.idToken());

        Optional<Usuario> existente = usuarioRepository.findByProvedorId(conta.id())
                .or(() -> usuarioRepository.findByEmailIgnoreCase(conta.email()));

        Usuario usuario;
        boolean novo;
        if (existente.isPresent()) {
            usuario = existente.get();
            if (!usuario.isAtivo()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Esta conta está desativada.");
            }
            usuario.vincularGoogle(conta.id());
            novo = false;
        } else {
            usuario = Usuario.comGoogle(conta.nome(), conta.email(), conta.id());
            usuarioRepository.saveAndFlush(usuario);
            perfilService.criarVazio(usuario.getId());
            novo = true;
        }

        return responder(usuario, Dispositivo.de(req.dispositivo()), novo);
    }

    /**
     * Troca a senha de quem está logado.
     * Por segurança, encerra as sessões de TODOS os dispositivos e devolve tokens novos
     * só para o dispositivo atual (quem trocou continua logado).
     */
    @Transactional
    public TokensResponse trocarSenha(UUID usuarioId, TrocarSenhaRequest req) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, SESSAO_INVALIDA));

        if (!usuario.temSenha()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Sua conta entra pelo Google, então não tem senha para trocar.");
        }
        if (!passwordEncoder.matches(req.senhaAtual(), usuario.getSenhaHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(req.novaSenha(), usuario.getSenhaHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A nova senha precisa ser diferente da atual.");
        }

        usuario.trocarSenha(passwordEncoder.encode(req.novaSenha()));
        usuarioRepository.saveAndFlush(usuario);
        sessaoRepository.revogarTodas(usuarioId);

        return responder(usuario, Dispositivo.de(req.dispositivo()), false);
    }

    private TokensResponse responder(Usuario usuario, Dispositivo dispositivo, boolean novoUsuario) {
        TokenService.Tokens tokens = tokenService.gerar(usuario, dispositivo);
        return new TokensResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.expiraEmSegundos(),
                new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail()),
                novoUsuario
        );
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
