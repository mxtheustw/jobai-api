package com.vagas.contas.aceite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Uma linha da tabela "aceites": quem declarou o quê, em qual versão e quando. */
@Entity
@Table(name = "aceites")
public class Aceite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAceite tipo;

    @Column(nullable = false, length = 20)
    private String versao;

    @Column(name = "aceito_em", nullable = false, updatable = false)
    private Instant aceitoEm = Instant.now();

    protected Aceite() {
    }

    public Aceite(UUID usuarioId, TipoAceite tipo, String versao) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.versao = versao;
    }

    public TipoAceite getTipo() {
        return tipo;
    }

    public String getVersao() {
        return versao;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }
}
