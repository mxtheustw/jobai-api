package com.vagas.contas.plano;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Descobre se a pessoa tem um plano pago ativo.
 * Enquanto não existem pagamentos, todo mundo é Grátis.
 */
@Service
public class PlanoService {

    private final EntityManager entityManager;
    private final boolean liberarTudoParaTeste;

    public PlanoService(EntityManager entityManager,
                        @Value("${app.planos.liberar-tudo-para-teste:false}") boolean liberarTudoParaTeste) {
        this.entityManager = entityManager;
        this.liberarTudoParaTeste = liberarTudoParaTeste;
    }

    @Transactional(readOnly = true)
    public boolean temPlanoPago(UUID usuarioId) {
        if (liberarTudoParaTeste) {
            return true;
        }
        Object resultado = entityManager.createNativeQuery("""
                        select exists (
                            select 1 from assinaturas a
                            join planos p on p.id = a.plano_id
                            where a.usuario_id = :usuarioId
                              and a.status = 'ATIVA'
                              and p.codigo <> 'GRATIS'
                              and (a.fim_periodo_em is null or a.fim_periodo_em > now())
                        )
                        """)
                .setParameter("usuarioId", usuarioId)
                .getSingleResult();
        return Boolean.TRUE.equals(resultado);
    }
}
