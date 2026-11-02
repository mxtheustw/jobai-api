package com.vagas.contas.senha;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenEmailRepository extends JpaRepository<TokenEmail, UUID> {

    Optional<TokenEmail> findByTokenHash(String tokenHash);

    /** Invalida os links antigos ainda não usados (só o último pedido vale). */
    @Modifying
    @Query("update TokenEmail t set t.usadoEm = :agora "
            + "where t.usuarioId = :usuarioId and t.tipo = :tipo and t.usadoEm is null")
    int invalidarAnteriores(@Param("usuarioId") UUID usuarioId, @Param("tipo") String tipo,
                            @Param("agora") Instant agora);
}
