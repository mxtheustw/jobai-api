package com.vagas.contas;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Trocar senha logado e "esqueci minha senha". */
class SenhaTest extends TesteBase {

    private static final Pattern TOKEN_NO_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    /** Pede o link e devolve o token que estaria no e-mail. */
    private String pedirLinkEPegarToken(String email) throws Exception {
        postJson("/auth/esqueci-senha", Map.of("email", email)).andExpect(status().isOk());

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(envioEmail).enviar(eq(email), any(), anyString(), anyString(), texto.capture());
        Matcher m = TOKEN_NO_LINK.matcher(texto.getValue());
        assertThat(m.find()).as("o e-mail tem o link com o token").isTrue();
        return m.group(1);
    }

    @Test
    void esqueciMinhaSenhaFluxoCompleto() throws Exception {
        String email = emailNovo();
        cadastrar(email);
        String token = pedirLinkEPegarToken(email);

        postJson("/auth/redefinir-senha", Map.of("token", token, "novaSenha", "senhaNovinha456"))
                .andExpect(status().isNoContent());

        login(email, SENHA).andExpect(status().isUnauthorized());
        login(email, "senhaNovinha456").andExpect(status().isOk());
    }

    @Test
    void linkSoFuncionaUmaVez() throws Exception {
        String email = emailNovo();
        cadastrar(email);
        String token = pedirLinkEPegarToken(email);

        postJson("/auth/redefinir-senha", Map.of("token", token, "novaSenha", "senhaNovinha456"))
                .andExpect(status().isNoContent());
        postJson("/auth/redefinir-senha", Map.of("token", token, "novaSenha", "outraSenha789"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redefinirEncerraAsSessoesAntigas() throws Exception {
        String email = emailNovo();
        String refresh = ler(cadastrar(email), "$.refreshToken");
        String token = pedirLinkEPegarToken(email);

        postJson("/auth/redefinir-senha", Map.of("token", token, "novaSenha", "senhaNovinha456"))
                .andExpect(status().isNoContent());
        postJson("/auth/refresh", Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInventadoERecusado() throws Exception {
        postJson("/auth/redefinir-senha", Map.of("token", "token-inventado", "novaSenha", "senhaNovinha456"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailSemContaRespondeIgualMasNaoEnviaNada() throws Exception {
        String email = emailNovo();
        postJson("/auth/esqueci-senha", Map.of("email", email)).andExpect(status().isOk());

        verify(envioEmail, never()).enviar(eq(email), any(), anyString(), anyString(), anyString());
    }

    private ResultActions trocarSenha(String token, String atual, String nova) throws Exception {
        return mvc.perform(put("/me/senha").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(json.writeValueAsString(Map.of("senhaAtual", atual, "novaSenha", nova))));
    }

    @Test
    void trocarSenhaLogadoExigeASenhaAtual() throws Exception {
        String email = emailNovo();
        String token = ler(cadastrar(email), "$.accessToken");

        trocarSenha(token, "senhaErrada999", "senhaNovinha456").andExpect(status().isBadRequest());
        trocarSenha(token, SENHA, "senhaNovinha456").andExpect(status().isOk());

        login(email, "senhaNovinha456").andExpect(status().isOk());
    }
}
