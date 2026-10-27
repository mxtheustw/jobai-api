package com.vagas.contas.vaga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VagaRepository extends JpaRepository<Vaga, UUID> {

    /** Busca, de uma vez, as vagas já conhecidas de uma fonte (para atualizar em vez de duplicar). */
    List<Vaga> findByFonteAndIdFonteIn(String fonte, Collection<String> idsFonte);

    /** Desativa as vagas de uma fonte que o coletor não encontra desde a data limite. */
    @Modifying
    @Query("update Vaga v set v.ativa = false, v.atualizadaEm = :agora "
            + "where v.ativa = true and v.fonte = :fonte and v.vistaPorUltimoEm < :limite")
    int desativarNaoVistas(@Param("fonte") String fonte, @Param("limite") Instant limite, @Param("agora") Instant agora);

    /** Desativa as vagas cujo prazo de inscrição já passou. */
    @Modifying
    @Query("update Vaga v set v.ativa = false, v.atualizadaEm = :agora "
            + "where v.ativa = true and v.prazoInscricao is not null and v.prazoInscricao < :hoje")
    int desativarComPrazoVencido(@Param("hoje") LocalDate hoje, @Param("agora") Instant agora);
}
