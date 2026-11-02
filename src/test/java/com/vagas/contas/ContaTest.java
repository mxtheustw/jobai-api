package com.vagas.contas;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exportar dados (LGPD) e excluir conta. */
class ContaTest extends TesteBase {

    @Test
    void exportarTrazOsDadosESemSenha() throws Exception {
        String email = emailNovo();
        String token = ler(cadastrar(email), "$.accessToken");

        getComToken("/me/exportar", token)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("meus-dados-jobai.json")))
                .andExpect(jsonPath("$.conta.email").value(email))
                .andExpect(jsonPath("$.perfil.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.plano").value("GRATIS"))
                .andExpect(jsonPath("$.aceites.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.sessoes.length()").value(1))
                .andExpect(content().string(not(containsString("senhaHash"))))
                .andExpect(content().string(not(containsString("$2a$"))));  // começo de toda senha BCrypt
    }

    @Test
    void excluirContaExigeSenhaEApagaTudo() throws Exception {
        String email = emailNovo();
        String token = ler(cadastrar(email), "$.accessToken");

        deleteComToken("/me", Map.of("senha", "senhaErrada999"), token).andExpect(status().is4xxClientError());
        login(email, SENHA).andExpect(status().isOk());

        deleteComToken("/me", Map.of("senha", SENHA), token).andExpect(status().isNoContent());
        login(email, SENHA).andExpect(status().isUnauthorized());
    }
}
