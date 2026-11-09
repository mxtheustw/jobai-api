package com.vagas.contas.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma linha da tabela "usuarios".
 * Cada campo aqui é uma coluna da tabela.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private String email;

    /** Senha criptografada (BCrypt). Fica vazia para quem entra só com Google. */
    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provedor provedor;

    /** Identificador da pessoa no Google (ou Apple, no futuro). */
    @Column(name = "provedor_id")
    private String provedorId;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    /** Construtor vazio exigido pelo JPA. Não use direto. */
    protected Usuario() {
    }

    /** Cria uma conta de e-mail + senha. */
    public static Usuario comSenha(String nome, String email, String senhaHash) {
        Usuario u = new Usuario();
        u.nome = nome;
        u.email = email;
        u.senhaHash = senhaHash;
        u.provedor = Provedor.LOCAL;
        u.emailVerificado = false;
        return u;
    }

    /** Cria uma conta a partir do Google. O Google já confirmou o e-mail. */
    public static Usuario comGoogle(String nome, String email, String googleId) {
        Usuario u = new Usuario();
        u.nome = nome;
        u.email = email;
        u.provedor = Provedor.GOOGLE;
        u.provedorId = googleId;
        u.emailVerificado = true;
        return u;
    }

    /** Liga uma conta existente ao Google (ex.: criou com senha e depois entrou com Google). */
    public void vincularGoogle(String googleId) {
        if (this.provedorId == null) {
            this.provedorId = googleId;
        }
        this.emailVerificado = true;
    }

    /** A pessoa provou que o e-mail é dela (abriu um link que mandamos para ele). */
    public void confirmarEmail() {
        this.emailVerificado = true;
    }

    /** Apaga a senha (a conta passa a entrar só pelo Google). */
    public void removerSenha() {
        this.senhaHash = null;
    }

    /** Troca a senha (recebe a senha já criptografada). */
    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    /** A conta tem senha? (quem entrou só pelo Google não tem) */
    public boolean temSenha() {
        return senhaHash != null;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Provedor getProvedor() {
        return provedor;
    }

    public String getProvedorId() {
        return provedorId;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
