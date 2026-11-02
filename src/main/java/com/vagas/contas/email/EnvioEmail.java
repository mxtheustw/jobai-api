package com.vagas.contas.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envia e-mails pela API da Brevo (por HTTP: o Render grátis bloqueia o envio por SMTP).
 *
 * Sem BREVO_API_KEY configurada, não envia nada. No PC (SITE_URL com localhost), o conteúdo
 * aparece no log para dar para testar sem a Brevo.
 */
@Component
public class EnvioEmail {

    private static final Logger log = LoggerFactory.getLogger(EnvioEmail.class);
    private static final URI BREVO = URI.create("https://api.brevo.com/v3/smtp/email");

    private final String apiKey;
    private final String remetente;
    private final String remetenteNome;
    private final boolean ambienteLocal;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public EnvioEmail(@Value("${app.email.brevo-api-key:}") String apiKey,
                      @Value("${app.email.remetente:}") String remetente,
                      @Value("${app.email.remetente-nome:JobAI}") String remetenteNome,
                      @Value("${app.site.url:http://localhost:3000}") String siteUrl,
                      JsonMapper json) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remetente = remetente == null ? "" : remetente.trim();
        this.remetenteNome = remetenteNome;
        this.ambienteLocal = siteUrl.contains("localhost");
        this.json = json;
    }

    /** Envia o e-mail. Nunca lança erro: se falhar, só registra no log (quem pediu não fica sabendo). */
    public void enviar(String paraEmail, String paraNome, String assunto, String html, String texto) {
        if (apiKey.isEmpty() || remetente.isEmpty()) {
            if (ambienteLocal) {
                log.info("[E-MAIL DE TESTE, NÃO ENVIADO] Para: {} | Assunto: {}\n{}", paraEmail, assunto, texto);
            } else {
                log.error("E-mail não enviado: BREVO_API_KEY ou EMAIL_REMETENTE não configurados.");
            }
            return;
        }
        try {
            Map<String, Object> corpo = Map.of(
                    "sender", Map.of("name", remetenteNome, "email", remetente),
                    "to", List.of(Map.of("email", paraEmail, "name", paraNome == null ? paraEmail : paraNome)),
                    "subject", assunto,
                    "htmlContent", html,
                    "textContent", texto
            );
            HttpRequest req = HttpRequest.newBuilder(BREVO)
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.error("Brevo recusou o e-mail ({}): {}", resp.statusCode(), resp.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Envio de e-mail interrompido", e);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail pela Brevo", e);
        }
    }
}
