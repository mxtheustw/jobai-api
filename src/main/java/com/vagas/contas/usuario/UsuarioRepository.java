package com.vagas.contas.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Faz as consultas na tabela "usuarios".
 * O Spring cria o SQL sozinho a partir do nome de cada método.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByProvedorId(String provedorId);
}
