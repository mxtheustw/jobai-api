package com.vagas.contas.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<Sessao, UUID> {

    Optional<Sessao> findByTokenHash(String tokenHash);

    /** Encerra TODAS as sessões de uma pessoa (ex.: depois de trocar a senha). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Sessao s set s.revogado = true where s.usuarioId = :usuarioId and s.revogado = false")
    int revogarTodas(@Param("usuarioId") UUID usuarioId);
}
