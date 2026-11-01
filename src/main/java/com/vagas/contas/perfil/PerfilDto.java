package com.vagas.contas.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O perfil no formato JSON que o site envia e recebe.
 * É uma cópia do tipo Profile do front (frontend/src/types/profile.ts),
 * com os mesmos nomes de campos. Se o Lucas mudar o Profile, mude aqui também.
 */
public record PerfilDto(
        @NotBlank(message = "Informe seu nome.")
        @Size(max = 120, message = "O nome pode ter até 120 caracteres.")
        String nome,

        Local local,
        String momento,
        List<String> areas,
        List<String> niveis,
        List<String> contratos,
        List<String> modalidades,
        String alcance,
        List<String> turnos,
        Escolaridade escolaridade,
        String curso,
        List<String> experiencias,
        String experienciaDescricao,
        String ultimoCargo,
        String tempoExperiencia,
        List<String> habilidades,
        List<Idioma> idiomas,
        /** Até 3 cargos que a pessoa procura. Lista vazia = "ainda não sei". */
        List<String> cargosDesejados,
        /** Até 5 cidades onde a pessoa quer trabalhar (a cidade onde mora fica em "local"). */
        List<Local> cidadesTrabalho,
        /** true = aceita qualquer cidade (aí cidadesTrabalho vem vazia). */
        Boolean qualquerCidade,

        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Esse e-mail não parece válido.")
        String email
) {
    public record Local(String cidade, String uf) {
    }

    public record Escolaridade(String nivel, String status) {
    }

    public record Idioma(String idioma, String nivel) {
    }
}
