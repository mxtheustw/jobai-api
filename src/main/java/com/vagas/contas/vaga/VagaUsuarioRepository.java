package com.vagas.contas.vaga;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Consultas da tabela vagas_usuario. As que devolvem List<Object[]> trazem pares [VagaUsuario, Vaga].
 * Só entram vagas abertas (ativa) e não descartadas.
 */
public interface VagaUsuarioRepository extends JpaRepository<VagaUsuario, UUID> {

    String BASE = "from VagaUsuario vu join Vaga v on v.id = vu.vagaId "
            + "where vu.usuarioId = :usuarioId and v.ativa = true and vu.situacao <> 'DESCARTADA' ";

    /** Feed completo (planos pagos): todas as vagas com porcentagem, da maior para a menor. */
    @Query("select vu, v " + BASE + "and vu.percentual is not null "
            + "order by vu.percentual desc, v.publicadaEm desc nulls last")
    List<Object[]> comMatch(@Param("usuarioId") UUID usuarioId, Limit limite);

    /** Feed do Grátis: só as vagas que já foram liberadas para a pessoa. */
    @Query("select vu, v " + BASE + "and vu.percentual is not null and vu.liberadaEm is not null "
            + "order by vu.liberadaEm desc, vu.percentual desc")
    List<Object[]> liberadas(@Param("usuarioId") UUID usuarioId, Limit limite);

    /** Próximas vagas a liberar no Grátis: as de maior porcentagem que ainda não foram liberadas. */
    @Query("select vu " + BASE + "and vu.percentual is not null and vu.liberadaEm is null "
            + "order by vu.percentual desc, v.publicadaEm desc nulls last")
    List<VagaUsuario> candidatasALiberar(@Param("usuarioId") UUID usuarioId, Limit limite);

    /** Quantas vagas foram liberadas para a pessoa desde o começo da semana. */
    @Query("select count(vu) from VagaUsuario vu where vu.usuarioId = :usuarioId and vu.liberadaEm >= :desde")
    long liberadasDesde(@Param("usuarioId") UUID usuarioId, @Param("desde") Instant desde);

    /** "Outras vagas que encontramos": sem porcentagem, das mais recentes para as mais antigas. */
    @Query("select vu, v " + BASE + "and vu.percentual is null "
            + "order by v.publicadaEm desc nulls last")
    List<Object[]> semMatch(@Param("usuarioId") UUID usuarioId, Limit limite);

    /** Uma vaga específica da pessoa: lista com 0 ou 1 item [VagaUsuario, Vaga]. */
    @Query("select vu, v from VagaUsuario vu join Vaga v on v.id = vu.vagaId "
            + "where vu.usuarioId = :usuarioId and v.id = :vagaId")
    List<Object[]> umaVaga(@Param("usuarioId") UUID usuarioId, @Param("vagaId") UUID vagaId);

    List<VagaUsuario> findByUsuarioIdInAndVagaIdIn(Collection<UUID> usuarioIds, Collection<UUID> vagaIds);
}
