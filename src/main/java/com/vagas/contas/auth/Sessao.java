package com.vagas.contas.auth;

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

/**
 * Uma linha da tabela "sessoes": um login ativo em um dispositivo.
 * Guarda só o "hash" do refresh token, nunca o token em si.
 */
@Entity
@Table(name = "sessoes")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Dispositivo dispositivo;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean revogado = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected Sessao() {
    }

    public Sessao(UUID usuarioId, String tokenHash, Dispositivo dispositivo, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.dispositivo = dispositivo;
        this.expiraEm = expiraEm;
    }

    /** A sessão ainda pode ser usada? (não foi encerrada e não venceu) */
    public boolean isValida() {
        return !revogado && Instant.now().isBefore(expiraEm);
    }

    public void revogar() {
        this.revogado = true;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }
}
