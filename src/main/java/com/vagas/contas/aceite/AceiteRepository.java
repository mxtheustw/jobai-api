package com.vagas.contas.aceite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AceiteRepository extends JpaRepository<Aceite, UUID> {

    Optional<Aceite> findByUsuarioIdAndTipoAndVersao(UUID usuarioId, TipoAceite tipo, String versao);

    boolean existsByUsuarioIdAndTipoAndVersao(UUID usuarioId, TipoAceite tipo, String versao);
}
