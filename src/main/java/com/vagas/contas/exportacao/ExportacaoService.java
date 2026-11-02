package com.vagas.contas.exportacao;

import com.vagas.contas.aceite.AceiteRepository;
import com.vagas.contas.auth.SessaoRepository;
import com.vagas.contas.erro.ApiException;
import com.vagas.contas.perfil.PerfilDto;
import com.vagas.contas.perfil.PerfilService;
import com.vagas.contas.perfil.PerfilService.CurriculoInfo;
import com.vagas.contas.perfil.PerfilService.MeResponse;
import com.vagas.contas.plano.PlanoService;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import com.vagas.contas.vaga.MeVagasService;
import com.vagas.contas.vaga.VagasDtos.VagaComMatch;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * "Baixar meus dados" (LGPD, art. 18: acesso e portabilidade).
 * Junta tudo o que guardamos sobre a pessoa em um único JSON.
 *
 * Não entram: a senha (nem criptografada) e os códigos de login/sessão.
 * O arquivo do currículo continua em GET /me/curriculo.
 */
@Service
public class ExportacaoService {

    public record Conta(UUID id, String nome, String email, String formaDeEntrar, String idGoogle,
                        boolean emailVerificado, Instant criadaEm) {
    }

    public record AceiteItem(String tipo, String versao, Instant aceitoEm) {
    }

    public record SessaoItem(String dispositivo, Instant iniciadaEm, Instant expiraEm, boolean ativa) {
    }

    public record Exportacao(Instant geradoEm, String sobre, Conta conta, PerfilDto perfil, CurriculoInfo curriculo,
                             String plano, List<AceiteItem> aceites, List<SessaoItem> sessoes,
                             List<VagaComMatch> vagasComMatch, List<VagaComMatch> outrasVagas) {
    }

    private static final String SOBRE = "Estes são os dados que o JobAI guarda sobre você. "
            + "Não incluímos sua senha (nem a versão criptografada) nem os códigos de login. "
            + "O arquivo do seu currículo, se você enviou, pode ser baixado em 'Meu perfil'. "
            + "Dúvidas: jobai.contato@gmail.com";

    private final UsuarioRepository usuarioRepository;
    private final PerfilService perfilService;
    private final PlanoService planoService;
    private final AceiteRepository aceiteRepository;
    private final SessaoRepository sessaoRepository;
    private final MeVagasService meVagasService;

    public ExportacaoService(UsuarioRepository usuarioRepository, PerfilService perfilService,
                             PlanoService planoService, AceiteRepository aceiteRepository,
                             SessaoRepository sessaoRepository, MeVagasService meVagasService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilService = perfilService;
        this.planoService = planoService;
        this.aceiteRepository = aceiteRepository;
        this.sessaoRepository = sessaoRepository;
        this.meVagasService = meVagasService;
    }

    @Transactional
    public Exportacao exportar(UUID usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
        MeResponse me = perfilService.buscar(usuarioId);

        Conta conta = new Conta(u.getId(), u.getNome(), u.getEmail(), formaDeEntrar(u),
                u.getProvedorId(), u.isEmailVerificado(), u.getCriadoEm());

        List<AceiteItem> aceites = aceiteRepository.findByUsuarioIdOrderByAceitoEmAsc(usuarioId).stream()
                .map(a -> new AceiteItem(a.getTipo().name(), a.getVersao(), a.getAceitoEm()))
                .toList();

        List<SessaoItem> sessoes = sessaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(s -> new SessaoItem(s.getDispositivo().name(), s.getCriadoEm(), s.getExpiraEm(), s.isValida()))
                .toList();

        String plano = planoService.temPlanoPago(usuarioId) ? "PAGO" : "GRATIS";

        return new Exportacao(Instant.now(), SOBRE, conta, me.perfil(), me.curriculo(), plano, aceites, sessoes,
                meVagasService.listarTodasParaExportar(usuarioId),
                meVagasService.outrasTodasParaExportar(usuarioId));
    }

    private static String formaDeEntrar(Usuario u) {
        return switch (u.getProvedor()) {
            case LOCAL -> u.temSenha() ? "E-mail e senha" : "E-mail";
            case GOOGLE -> u.temSenha() ? "Google e e-mail e senha" : "Google";
            case APPLE -> "Apple";
        };
    }
}
