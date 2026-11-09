package com.vagas.contas;

import com.vagas.contas.auth.GoogleTokenVerifier.ContaGoogle;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Confirmação de e-mail e a proteção do login com Google. */
class ConfirmacaoEmailTest extends TesteBase {

    private static final String ASSUNTO = "Confirme seu e-mail no JobAI";

    @Test
    void cadastroMandaLinkEConfirmarFunciona() throws Exception {
        String email = emailNovo();
        String accessToken = ler(cadastrar(email), "$.accessToken");

        getComToken("/me", accessToken).andExpect(jsonPath("$.emailVerificado").value(false));

        String token = tokenDoEmail(email, ASSUNTO);
        postJson("/auth/confirmar-email", Map.of("token", token)).andExpect(status().isNoContent());

        getComToken("/me", accessToken).andExpect(jsonPath("$.emailVerificado").value(true));
    }

    @Test
    void linkDeConfirmacaoSoFuncionaUmaVez() throws Exception {
        String email = emailNovo();
        cadastrar(email);
        String token = tokenDoEmail(email, ASSUNTO);

        postJson("/auth/confirmar-email", Map.of("token", token)).andExpect(status().isNoContent());
        postJson("/auth/confirmar-email", Map.of("token", token)).andExpect(status().isBadRequest());
    }

    @Test
    void linkDeSenhaNaoServeParaConfirmarEmail() throws Exception {
        String email = emailNovo();
        cadastrar(email);
        postJson("/auth/esqueci-senha", Map.of("email", email)).andExpect(status().isOk());
        String tokenDeSenha = tokenDoEmail(email, "Redefinir sua senha do JobAI");

        postJson("/auth/confirmar-email", Map.of("token", tokenDeSenha)).andExpect(status().isBadRequest());
    }

    @Test
    void reenviarMandaUmLinkNovoEOAntigoParaDeValer() throws Exception {
        String email = emailNovo();
        String accessToken = ler(cadastrar(email), "$.accessToken");
        String primeiro = tokenDoEmail(email, ASSUNTO);

        mvc.perform(post("/me/reenviar-confirmacao").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        String segundo = tokenDoEmail(email, ASSUNTO);

        postJson("/auth/confirmar-email", Map.of("token", primeiro)).andExpect(status().isBadRequest());
        postJson("/auth/confirmar-email", Map.of("token", segundo)).andExpect(status().isNoContent());
    }

    @Test
    void reenviarExigeLogin() throws Exception {
        mvc.perform(post("/me/reenviar-confirmacao")).andExpect(status().isUnauthorized());
    }

    @Test
    void redefinirSenhaTambemConfirmaOEmail() throws Exception {
        String email = emailNovo();
        cadastrar(email);
        postJson("/auth/esqueci-senha", Map.of("email", email)).andExpect(status().isOk());
        String token = tokenDoEmail(email, "Redefinir sua senha do JobAI");
        postJson("/auth/redefinir-senha", Map.of("token", token, "novaSenha", "senhaNovinha456"))
                .andExpect(status().isNoContent());

        String accessToken = ler(login(email, "senhaNovinha456").andReturn().getResponse().getContentAsString(),
                "$.accessToken");
        getComToken("/me", accessToken).andExpect(jsonPath("$.emailVerificado").value(true));
    }

    @Test
    void googleEntraNaContaETiraASenhaDeQuemNaoConfirmou() throws Exception {
        // Alguém cria a conta com o e-mail da vítima e uma senha, sem confirmar o e-mail...
        String email = emailNovo();
        String refreshDoIntruso = ler(cadastrar(email), "$.refreshToken");

        // ...depois a dona do e-mail entra com o Google
        when(googleTokenVerifier.verificar(anyString()))
                .thenReturn(new ContaGoogle("google-" + UUID.randomUUID(), email, "Dona Do Email"));
        postJson("/auth/google", Map.of("idToken", "token-do-google", "dispositivo", "WEB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // A senha do intruso não funciona mais, e a sessão dele caiu
        login(email, SENHA).andExpect(status().isUnauthorized());
        postJson("/auth/refresh", Map.of("refreshToken", refreshDoIntruso)).andExpect(status().isUnauthorized());
    }

    @Test
    void googleMantemASenhaDeQuemJaConfirmou() throws Exception {
        String email = emailNovo();
        cadastrar(email);
        postJson("/auth/confirmar-email", Map.of("token", tokenDoEmail(email, ASSUNTO)))
                .andExpect(status().isNoContent());

        when(googleTokenVerifier.verificar(anyString()))
                .thenReturn(new ContaGoogle("google-" + UUID.randomUUID(), email, "Ana Souza"));
        postJson("/auth/google", Map.of("idToken", "token-do-google")).andExpect(status().isOk());

        // Conta confirmada: a senha continua valendo
        login(email, SENHA).andExpect(status().isOk());
    }
}
