package com.vagas.contas.perfil;

import com.vagas.contas.erro.ApiException;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Regras do perfil: converte o JSON do site (PerfilDto) para a tabela (Perfil) e vice-versa.
 */
@Service
public class PerfilService {

    private static final TypeReference<List<PerfilDto.Idioma>> LISTA_IDIOMAS = new TypeReference<>() {
    };

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final JsonMapper json;

    public PerfilService(PerfilRepository perfilRepository,
                         UsuarioRepository usuarioRepository,
                         JsonMapper json) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.json = json;
    }

    /** O que o GET /me devolve: o id da conta + o perfil no formato do site. */
    public record MeResponse(UUID id, String provedor, boolean emailVerificado, PerfilDto perfil) {
    }

    /** Cria ou substitui o perfil de um usuário com os dados vindos do site. */
    @Transactional
    public void salvar(UUID usuarioId, PerfilDto dto) {
        Perfil perfil = perfilRepository.findById(usuarioId).orElseGet(() -> new Perfil(usuarioId));
        aplicar(perfil, dto);
        perfilRepository.saveAndFlush(perfil);
    }

    /** Cria um perfil em branco (usado quando a pessoa entra com Google pela primeira vez). */
    @Transactional
    public void criarVazio(UUID usuarioId) {
        if (!perfilRepository.existsById(usuarioId)) {
            perfilRepository.saveAndFlush(new Perfil(usuarioId));
        }
    }

    @Transactional(readOnly = true)
    public MeResponse buscar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
        Perfil perfil = perfilRepository.findById(usuarioId).orElseGet(() -> new Perfil(usuarioId));
        return new MeResponse(usuario.getId(), usuario.getProvedor().name(),
                usuario.isEmailVerificado(), paraDto(usuario, perfil));
    }

    /** PUT /me/perfil: atualiza o perfil (e o nome). O e-mail não muda por aqui. */
    @Transactional
    public MeResponse atualizar(UUID usuarioId, PerfilDto dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
        if (dto.nome() != null && !dto.nome().isBlank()) {
            usuario.setNome(dto.nome().trim());
        }
        salvar(usuarioId, dto);
        return buscar(usuarioId);
    }

    // ----------------------------------------------------------------
    // Conversões
    // ----------------------------------------------------------------

    private void aplicar(Perfil p, PerfilDto dto) {
        if (dto.local() != null) {
            p.setCidade(texto(dto.local().cidade()));
            String uf = texto(dto.local().uf());
            p.setUf(uf == null ? null : uf.toUpperCase(Locale.ROOT));
        } else {
            p.setCidade(null);
            p.setUf(null);
        }
        p.setMomento(texto(dto.momento()));
        p.setAreas(lista(dto.areas()));
        p.setNiveis(lista(dto.niveis()));
        p.setContratos(lista(dto.contratos()));
        p.setModalidades(lista(dto.modalidades()));
        p.setAlcance(texto(dto.alcance()));
        p.setTurnos(lista(dto.turnos()));
        if (dto.escolaridade() != null) {
            p.setEscolaridadeNivel(texto(dto.escolaridade().nivel()));
            p.setEscolaridadeStatus(texto(dto.escolaridade().status()));
        } else {
            p.setEscolaridadeNivel(null);
            p.setEscolaridadeStatus(null);
        }
        p.setCurso(texto(dto.curso()));
        p.setExperiencias(lista(dto.experiencias()));
        p.setExperienciaDescricao(texto(dto.experienciaDescricao()));
        p.setUltimoCargo(texto(dto.ultimoCargo()));
        p.setTempoExperiencia(texto(dto.tempoExperiencia()));
        p.setHabilidades(lista(dto.habilidades()));
        List<PerfilDto.Idioma> idiomas = dto.idiomas() == null ? List.of() : dto.idiomas();
        p.setIdiomas(json.writeValueAsString(idiomas));
    }

    private PerfilDto paraDto(Usuario u, Perfil p) {
        PerfilDto.Local local = (p.getCidade() == null && p.getUf() == null)
                ? null
                : new PerfilDto.Local(p.getCidade(), p.getUf() == null ? null : p.getUf().trim());
        PerfilDto.Escolaridade escolaridade = (p.getEscolaridadeNivel() == null)
                ? null
                : new PerfilDto.Escolaridade(p.getEscolaridadeNivel(), p.getEscolaridadeStatus());
        List<PerfilDto.Idioma> idiomas = json.readValue(p.getIdiomas(), LISTA_IDIOMAS);

        return new PerfilDto(
                u.getNome(),
                local,
                p.getMomento(),
                p.getAreas(),
                p.getNiveis(),
                p.getContratos(),
                p.getModalidades(),
                p.getAlcance(),
                p.getTurnos(),
                escolaridade,
                p.getCurso(),
                p.getExperiencias(),
                p.getExperienciaDescricao(),
                p.getUltimoCargo(),
                p.getTempoExperiencia(),
                p.getHabilidades(),
                idiomas,
                u.getEmail()
        );
    }

    /** Tira espaços extras e transforma texto vazio em "sem valor". */
    private static String texto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    /** Limpa uma lista: tira itens vazios e repetidos. */
    private static List<String> lista(List<String> valores) {
        List<String> limpa = new ArrayList<>();
        if (valores != null) {
            for (String v : valores) {
                String t = texto(v);
                if (t != null && !limpa.contains(t)) {
                    limpa.add(t);
                }
            }
        }
        return limpa;
    }
}
