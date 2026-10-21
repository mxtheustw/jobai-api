package com.vagas.contas.perfil;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Faz as consultas na tabela "perfis". */
public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
}
