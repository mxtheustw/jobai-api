package com.vagas.contas.conta;

import com.vagas.contas.curriculo.ArmazenamentoCurriculos;
import com.vagas.contas.erro.ApiException;
import com.vagas.contas.perfil.Perfil;
import com.vagas.contas.perfil.PerfilRepository;
import com.vagas.contas.seguranca.LimiteTentativas;
import com.vagas.contas.usuario.Usuario;
import com.vagas.contas.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.UUID;

/**
 * Exclusão de conta (direito garantido pela LGPD).
 *
 * Apaga o usuário e, em cascata no banco: perfil, sessões, aceites, assinaturas,
 * vagas e tokens de e-mail. Depois apaga também o arquivo do currículo.
 */
@Service
public class ContaService {

    private static final Logger log = LoggerFactory.getLogger(ContaService.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final ArmazenamentoCurriculos armazenamento;
    private final PasswordEncoder passwordEncoder;
    private final LimiteTentativas limiteTentativas;

    public ContaService(UsuarioRepository usuarioRepository,
                        PerfilRepository perfilRepository,
                        ArmazenamentoCurriculos armazenamento,
                        PasswordEncoder passwordEncoder,
                        LimiteTentativas limiteTentativas) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.armazenamento = armazenamento;
        this.passwordEncoder = passwordEncoder;
        this.limiteTentativas = limiteTentativas;
    }

    /**
     * @param senha obrigatória para contas com senha (confirma que é a própria pessoa);
     *              ignorada para contas que entram só pelo Google.
     */
    @Transactional
    public void excluir(UUID usuarioId, String senha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada."));

        if (usuario.temSenha()) {
            if (senha == null || senha.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Digite sua senha para confirmar a exclusão.");
            }
            String chave = "excluir:" + usuarioId;
            limiteTentativas.verificar(chave);
            if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                limiteTentativas.registrarFalha(chave);
                throw new ApiException(HttpStatus.BAD_REQUEST, "Senha incorreta. A conta não foi excluída.");
            }
        }

        String chaveCurriculo = perfilRepository.findById(usuarioId)
                .map(Perfil::getCurriculoChave)
                .orElse(null);

        // Apaga do banco (o resto vai junto em cascata: ON DELETE CASCADE nas tabelas)
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();

        // Só apaga o arquivo depois que o banco confirmar a exclusão
        if (chaveCurriculo != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        armazenamento.apagar(chaveCurriculo);
                    } catch (IOException | RuntimeException e) {
                        log.warn("Conta excluída, mas não consegui apagar o currículo {}", chaveCurriculo, e);
                    }
                }
            });
        }
        log.info("Conta {} excluída a pedido do usuário", usuarioId);
    }
}
