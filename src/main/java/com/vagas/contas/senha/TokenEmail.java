package com.vagas.contas.senha;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da tabela "tokens_email" (criada na V1): códigos que vão por e-mail,
 * como o link de redefinir senha. Guarda só o hash do código.
 */
@Entity
@Table(name = "tokens_email")
public class TokenEmail {

    public static final String RESET_SENHA = "RESET_SENHA";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected TokenEmail() {
    }

    public TokenEmail(UUID usuarioId, String tokenHash, String tipo, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.tipo = tipo;
        this.expiraEm = expiraEm;
    }

    public boolean isValido() {
        return usadoEm == null && Instant.now().isBefore(expiraEm);
    }

    public void marcarUsado() {
        this.usadoEm = Instant.now();
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getTipo() {
        return tipo;
    }
}
