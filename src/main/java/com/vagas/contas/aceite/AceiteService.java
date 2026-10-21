package com.vagas.contas.aceite;

import com.vagas.contas.erro.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra as declarações (aceites). Nunca guarda a idade, só a declaração,
 * a versão dos termos e a data.
 */
@Service
public class AceiteService {

    private final AceiteRepository aceiteRepository;
    private final String versaoAtual;

    public AceiteService(AceiteRepository aceiteRepository,
                         @Value("${app.termos.versao-atual}") String versaoAtual) {
        this.aceiteRepository = aceiteRepository;
        this.versaoAtual = versaoAtual;
    }

    public record AceiteResponse(String tipo, String versao, Instant aceitoEm) {
    }

    public String getVersaoAtual() {
        return versaoAtual;
    }

    /** Confere se a versão enviada pelo site é a versão atual dos termos. */
    public void validarVersao(String versao) {
        if (versao == null || !versaoAtual.equals(versao.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Os termos foram atualizados. Recarregue a página e aceite a versão atual.");
        }
    }

    /** Registra o aceite. Se a pessoa já aceitou essa versão, devolve o registro existente. */
    @Transactional
    public AceiteResponse registrar(UUID usuarioId, TipoAceite tipo, String versao) {
        validarVersao(versao);
        String v = versao.trim();
        Aceite aceite = aceiteRepository.findByUsuarioIdAndTipoAndVersao(usuarioId, tipo, v)
                .orElseGet(() -> aceiteRepository.saveAndFlush(new Aceite(usuarioId, tipo, v)));
        return new AceiteResponse(aceite.getTipo().name(), aceite.getVersao(), aceite.getAceitoEm());
    }

    /** A pessoa já aceitou a versão ATUAL dos termos? */
    @Transactional(readOnly = true)
    public boolean aceitouTermos(UUID usuarioId) {
        return aceiteRepository.existsByUsuarioIdAndTipoAndVersao(usuarioId, TipoAceite.TERMOS, versaoAtual);
    }

    /** Bloqueia a ação se a pessoa ainda não aceitou os termos atuais. */
    @Transactional(readOnly = true)
    public void exigirTermos(UUID usuarioId) {
        if (!aceitouTermos(usuarioId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Aceite os termos de uso para continuar.");
        }
    }

    /** Para o futuro fluxo de pagamento: só assina plano pago quem fez a declaração. */
    @Transactional(readOnly = true)
    public void exigirDeclaracaoMaioridade(UUID usuarioId) {
        if (!aceiteRepository.existsByUsuarioIdAndTipoAndVersao(usuarioId, TipoAceite.MAIORIDADE_ASSINATURA, versaoAtual)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Para assinar um plano pago, confirme que você tem 18 anos ou mais, ou que é o responsável.");
        }
    }
}
