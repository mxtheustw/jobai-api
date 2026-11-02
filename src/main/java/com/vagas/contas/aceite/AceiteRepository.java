package com.vagas.contas.aceite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AceiteRepository extends JpaRepository<Aceite, UUID> {

    Optional<Aceite> findByUsuarioIdAndTipoAndVersao(UUID usuarioId, TipoAceite tipo, String versao);

    List<Aceite> findByUsuarioIdOrderByAceitoEmAsc(UUID usuarioId);

    boolean existsByUsuarioIdAndTipoAndVersao(UUID usuarioId, TipoAceite tipo, String versao);
}
