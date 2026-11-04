package com.vagas.contas.limpeza;

import com.vagas.contas.auth.SessaoRepository;
import com.vagas.contas.senha.TokenEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Limpeza automática do banco: apaga o que não serve mais para nada.
 * - sessões vencidas ou encerradas (logout, troca de senha, refresh já usado);
 * - links de "esqueci minha senha" vencidos ou já usados.
 *
 * Economiza espaço no Supabase grátis e segue a LGPD (não guardar dados além do necessário).
 * Roda 5 minutos depois que a API liga e, depois, a cada 6 horas.
 */
@Service
public class LimpezaService {

    private static final Logger log = LoggerFactory.getLogger(LimpezaService.class);

    private final SessaoRepository sessaoRepository;
    private final TokenEmailRepository tokenEmailRepository;

    public LimpezaService(SessaoRepository sessaoRepository, TokenEmailRepository tokenEmailRepository) {
        this.sessaoRepository = sessaoRepository;
        this.tokenEmailRepository = tokenEmailRepository;
    }

    public record Resultado(int sessoes, int links) {
    }

    @Scheduled(initialDelayString = "${app.limpeza.primeira-em:PT5M}", fixedDelayString = "${app.limpeza.intervalo:PT6H}")
    public void agendada() {
        try {
            Resultado r = limpar();
            if (r.sessoes() > 0 || r.links() > 0) {
                log.info("Limpeza do banco: {} sessões e {} links de senha apagados.", r.sessoes(), r.links());
            }
        } catch (Exception e) {
            log.error("Falha na limpeza do banco (tenta de novo no próximo horário)", e);
        }
    }

    @Transactional
    public Resultado limpar() {
        Instant agora = Instant.now();
        return new Resultado(sessaoRepository.apagarInuteis(agora), tokenEmailRepository.apagarInuteis(agora));
    }
}
