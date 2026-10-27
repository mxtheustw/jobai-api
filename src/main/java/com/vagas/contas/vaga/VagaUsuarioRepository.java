package com.vagas.contas.vaga;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VagaUsuarioRepository extends JpaRepository<VagaUsuario, UUID> {

    /**
     * Feed da pessoa: vagas abertas, sem as descartadas, da maior porcentagem para a menor.
     * Cada item é [VagaUsuario, Vaga].
     */
    @Query("select vu, v from VagaUsuario vu join Vaga v on v.id = vu.vagaId "
            + "where vu.usuarioId = :usuarioId and v.ativa = true and vu.situacao <> 'DESCARTADA' "
            + "order by vu.percentual desc nulls last, v.publicadaEm desc nulls last")
    List<Object[]> feed(@Param("usuarioId") UUID usuarioId, Limit limite);

    /** Uma vaga específica da pessoa: lista com 0 ou 1 item [VagaUsuario, Vaga]. */
    @Query("select vu, v from VagaUsuario vu join Vaga v on v.id = vu.vagaId "
            + "where vu.usuarioId = :usuarioId and v.id = :vagaId")
    List<Object[]> umaVaga(@Param("usuarioId") UUID usuarioId, @Param("vagaId") UUID vagaId);

    List<VagaUsuario> findByUsuarioIdInAndVagaIdIn(Collection<UUID> usuarioIds, Collection<UUID> vagaIds);
}
