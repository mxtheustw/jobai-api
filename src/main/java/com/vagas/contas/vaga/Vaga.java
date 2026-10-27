package com.vagas.contas.vaga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Uma linha da tabela "vagas" (criada na V6). */
@Entity
@Table(name = "vagas")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String fonte;

    @Column(name = "id_fonte", nullable = false, length = 100)
    private String idFonte;

    @Column(nullable = false, length = 300)
    private String titulo;

    private String empresa;
    private String cidade;

    @Column(columnDefinition = "char(2)")
    private String uf;

    private String modelo;
    private String contrato;
    private String senioridade;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private List<String> ferramentas = new ArrayList<>();

    @Column(name = "anos_experiencia")
    private Short anosExperiencia;

    @Column(name = "publicada_em")
    private LocalDate publicadaEm;

    @Column(name = "prazo_inscricao")
    private LocalDate prazoInscricao;

    @Column(nullable = false)
    private String url;

    private String descricao;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "vista_por_ultimo_em", nullable = false)
    private Instant vistaPorUltimoEm = Instant.now();

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm = Instant.now();

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm = Instant.now();

    protected Vaga() {
    }

    public Vaga(String fonte, String idFonte) {
        this.fonte = fonte;
        this.idFonte = idFonte;
    }

    /** Copia os dados que vieram do coletor e marca a vaga como vista agora. */
    public void atualizarCom(VagaColetada v) {
        this.titulo = v.titulo().trim();
        this.empresa = v.empresa();
        this.cidade = v.cidade();
        this.uf = v.uf();
        this.modelo = v.modelo();
        this.contrato = v.contrato();
        this.senioridade = v.senioridade();
        this.ferramentas = v.ferramentas() == null ? new ArrayList<>() : new ArrayList<>(v.ferramentas());
        this.anosExperiencia = v.anosExperiencia() == null ? null : v.anosExperiencia().shortValue();
        this.publicadaEm = v.publicadaEm();
        this.prazoInscricao = v.prazoInscricao();
        this.url = v.url().trim();
        this.descricao = v.descricao();
        this.ativa = true;
        this.vistaPorUltimoEm = Instant.now();
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFonte() { return fonte; }
    public String getIdFonte() { return idFonte; }
    public String getTitulo() { return titulo; }
    public String getEmpresa() { return empresa; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf == null ? null : uf.trim(); }
    public String getModelo() { return modelo; }
    public String getContrato() { return contrato; }
    public String getSenioridade() { return senioridade; }
    public List<String> getFerramentas() { return ferramentas; }
    public Short getAnosExperiencia() { return anosExperiencia; }
    public LocalDate getPublicadaEm() { return publicadaEm; }
    public LocalDate getPrazoInscricao() { return prazoInscricao; }
    public String getUrl() { return url; }
    public String getDescricao() { return descricao; }
    public boolean isAtiva() { return ativa; }
    public Instant getVistaPorUltimoEm() { return vistaPorUltimoEm; }
}
