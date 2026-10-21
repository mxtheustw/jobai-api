package com.vagas.contas.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa uma linha da tabela "perfis" (criada no V1 e ajustada no V2).
 * Cada usuário tem exatamente um perfil, com o mesmo id do usuário.
 */
@Entity
@Table(name = "perfis")
public class Perfil {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    private String cidade;

    @Column(columnDefinition = "char(2)")
    private String uf;

    private String momento;

    // Listas: no banco são colunas do tipo TEXT[]
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> areas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> niveis = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> contratos = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> modalidades = new ArrayList<>();

    private String alcance;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> turnos = new ArrayList<>();

    @Column(name = "escolaridade_nivel")
    private String escolaridadeNivel;

    @Column(name = "escolaridade_status")
    private String escolaridadeStatus;

    private String curso;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> experiencias = new ArrayList<>();

    @Column(name = "experiencia_descricao")
    private String experienciaDescricao;

    @Column(name = "ultimo_cargo")
    private String ultimoCargo;

    @Column(name = "tempo_experiencia")
    private String tempoExperiencia;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> habilidades = new ArrayList<>();

    /** Lista de idiomas guardada como JSON, ex.: [{"idioma":"Inglês","nivel":"basico"}] */
    @ColumnTransformer(write = "?::jsonb")
    @Column(columnDefinition = "jsonb", nullable = false)
    private String idiomas = "[]";

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Perfil() {
    }

    public Perfil(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    // ----- getters e setters -----

    public UUID getUsuarioId() { return usuarioId; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getMomento() { return momento; }
    public void setMomento(String momento) { this.momento = momento; }

    public List<String> getAreas() { return areas; }
    public void setAreas(List<String> areas) { this.areas = areas; }

    public List<String> getNiveis() { return niveis; }
    public void setNiveis(List<String> niveis) { this.niveis = niveis; }

    public List<String> getContratos() { return contratos; }
    public void setContratos(List<String> contratos) { this.contratos = contratos; }

    public List<String> getModalidades() { return modalidades; }
    public void setModalidades(List<String> modalidades) { this.modalidades = modalidades; }

    public String getAlcance() { return alcance; }
    public void setAlcance(String alcance) { this.alcance = alcance; }

    public List<String> getTurnos() { return turnos; }
    public void setTurnos(List<String> turnos) { this.turnos = turnos; }

    public String getEscolaridadeNivel() { return escolaridadeNivel; }
    public void setEscolaridadeNivel(String escolaridadeNivel) { this.escolaridadeNivel = escolaridadeNivel; }

    public String getEscolaridadeStatus() { return escolaridadeStatus; }
    public void setEscolaridadeStatus(String escolaridadeStatus) { this.escolaridadeStatus = escolaridadeStatus; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public List<String> getExperiencias() { return experiencias; }
    public void setExperiencias(List<String> experiencias) { this.experiencias = experiencias; }

    public String getExperienciaDescricao() { return experienciaDescricao; }
    public void setExperienciaDescricao(String experienciaDescricao) { this.experienciaDescricao = experienciaDescricao; }

    public String getUltimoCargo() { return ultimoCargo; }
    public void setUltimoCargo(String ultimoCargo) { this.ultimoCargo = ultimoCargo; }

    public String getTempoExperiencia() { return tempoExperiencia; }
    public void setTempoExperiencia(String tempoExperiencia) { this.tempoExperiencia = tempoExperiencia; }

    public List<String> getHabilidades() { return habilidades; }
    public void setHabilidades(List<String> habilidades) { this.habilidades = habilidades; }

    public String getIdiomas() { return idiomas; }
    public void setIdiomas(String idiomas) { this.idiomas = idiomas; }
}
