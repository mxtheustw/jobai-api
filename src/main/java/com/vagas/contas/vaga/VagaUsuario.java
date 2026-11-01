package com.vagas.contas.vaga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da tabela "vagas_usuario": o resultado do match de UMA vaga para UMA pessoa.
 * (criada na V1, com colunas do match na V3 e ligação com "vagas" na V6)
 */
@Entity
@Table(name = "vagas_usuario")
public class VagaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "vaga_id", nullable = false)
    private UUID vagaId;

    /** RECOMENDADA, SALVA, CANDIDATOU, DESCARTADA */
    @Column(nullable = false, length = 20)
    private String situacao = "RECOMENDADA";

    /** Porcentagem final do match (0 a 100). */
    @Column(name = "score_ia")
    private BigDecimal percentual;

    @Column(name = "nota_filtros")
    private BigDecimal notaFiltros;

    @Column(name = "nota_afinidade")
    private BigDecimal notaAfinidade;

    @Column(name = "por_que_combina")
    private String porQueCombina;

    /** Lista do que falta, em JSON: [{"item":"...","tipo":"...","impacto":10}] */
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "o_que_falta", columnDefinition = "jsonb", nullable = false)
    private String oQueFalta = "[]";

    @Column(name = "calculado_em")
    private Instant calculadoEm;

    /** Quando a vaga foi liberada para a pessoa ver (controle das 3 por semana do Grátis). */
    @Column(name = "liberada_em")
    private Instant liberadaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected VagaUsuario() {
    }

    public VagaUsuario(UUID usuarioId, UUID vagaId) {
        this.usuarioId = usuarioId;
        this.vagaId = vagaId;
    }

    public void definirMatch(BigDecimal percentual, BigDecimal notaFiltros, BigDecimal notaAfinidade,
                             String porQueCombina, String oQueFaltaJson) {
        this.percentual = percentual;
        this.notaFiltros = notaFiltros;
        this.notaAfinidade = notaAfinidade;
        this.porQueCombina = porQueCombina;
        this.oQueFalta = oQueFaltaJson;
        this.calculadoEm = Instant.now();
    }

    public void liberar() {
        if (this.liberadaEm == null) {
            this.liberadaEm = Instant.now();
        }
    }

    public Instant getLiberadaEm() { return liberadaEm; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getVagaId() { return vagaId; }
    public String getSituacao() { return situacao; }
    public BigDecimal getPercentual() { return percentual; }
    public String getPorQueCombina() { return porQueCombina; }
    public String getOQueFalta() { return oQueFalta; }
}
