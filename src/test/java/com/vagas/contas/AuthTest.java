package com.vagas.contas;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro, login, renovação de sessão e bloqueio por senhas erradas. */
class AuthTest extends TesteBase {

    @Test
    void cadastroDevolveTokensEContaNova() throws Exception {
        String email = emailNovo();
        String resposta = cadastrar(email);

        assertThat(ler(resposta, "$.accessToken")).isNotBlank();
        assertThat(ler(resposta, "$.refreshToken")).isNotBlank();
        assertThat(ler(resposta, "$.usuario.email")).isEqualTo(email);
        assertThat(ler(resposta, "$.novoUsuario")).isEqualTo("true");
    }

    @Test
    void naoDeixaCadastrarOMesmoEmailDuasVezes() throws Exception {
        String email = emailNovo();
        cadastrar(email);

        postJson("/auth/cadastro", Map.of(
                "perfil", perfil(email), "senha", SENHA, "aceiteTermos", Map.of("versao", "2026-10")))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroSemAceitarOsTermosERecusado() throws Exception {
        postJson("/auth/cadastro", Map.of("perfil", perfil(emailNovo()), "senha", SENHA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void senhaCurtaERecusada() throws Exception {
        postJson("/auth/cadastro", Map.of(
                "perfil", perfil(emailNovo()), "senha", "curta", "aceiteTermos", Map.of("versao", "2026-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void loginComSenhaCertaEErrada() throws Exception {
        String email = emailNovo();
        cadastrar(email);

        login(email, SENHA).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists());
        login(email, "senhaErrada999").andExpect(status().isUnauthorized());
    }

    @Test
    void emailInexistenteDaOMesmoErroQueSenhaErrada() throws Exception {
        login(emailNovo(), SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void bloqueiaDepoisDe5SenhasErradas() throws Exception {
        String email = emailNovo();
        cadastrar(email);

        for (int i = 0; i < 5; i++) {
            login(email, "senhaErrada999").andExpect(status().isUnauthorized());
        }
        // 6ª tentativa: bloqueado, mesmo com a senha certa
        login(email, SENHA).andExpect(status().isTooManyRequests());
    }

    @Test
    void refreshTokenSoPodeSerUsadoUmaVez() throws Exception {
        String refresh = ler(cadastrar(emailNovo()), "$.refreshToken");

        postJson("/auth/refresh", Map.of("refreshToken", refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").exists());
        // O mesmo refresh de novo: recusado (foi trocado por um novo)
        postJson("/auth/refresh", Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        String refresh = ler(cadastrar(emailNovo()), "$.refreshToken");

        postJson("/auth/logout", Map.of("refreshToken", refresh)).andExpect(status().isNoContent());
        postJson("/auth/refresh", Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void rotasDoMeExigemLogin() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/me").header("Authorization", "Bearer token-inventado")).andExpect(status().isUnauthorized());
    }

    @Test
    void meDevolveOPerfilDaPessoaLogada() throws Exception {
        String email = emailNovo();
        String token = ler(cadastrar(email), "$.accessToken");

        getComToken("/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termosAceitos").value(true))
                .andExpect(jsonPath("$.perfil.nome").value("Ana Souza"));
    }

    @Test
    void saudeRespondeSemLogin() throws Exception {
        mvc.perform(get("/saude")).andExpect(status().isOk());
    }

    @Test
    void jsonQuebradoDa400() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ isso não é json"))
                .andExpect(status().isBadRequest());
    }
}
