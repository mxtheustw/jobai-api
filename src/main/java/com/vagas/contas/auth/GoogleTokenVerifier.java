package com.vagas.contas.auth;

import com.vagas.contas.erro.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Confere se o idToken que o site recebeu do Google é verdadeiro:
 * assinado pelo Google, ainda válido e emitido para o NOSSO app (GOOGLE_CLIENT_ID).
 */
@Component
public class GoogleTokenVerifier {

    private static final String CHAVES_DO_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISSORES = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private volatile JwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public record ContaGoogle(String id, String email, String nome) {
    }

    public ContaGoogle verificar(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Entrar com Google ainda não foi configurado.");
        }

        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Não foi possível confirmar sua conta Google. Tente de novo.");
        }

        String email = jwt.getClaimAsString("email");
        Boolean emailVerificado = jwt.getClaimAsBoolean("email_verified");
        if (email == null || !Boolean.TRUE.equals(emailVerificado)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Sua conta Google precisa ter um e-mail confirmado.");
        }

        String nome = jwt.getClaimAsString("name");
        if (nome == null || nome.isBlank()) {
            nome = email.substring(0, email.indexOf('@'));
        }
        return new ContaGoogle(jwt.getSubject(), email.toLowerCase(Locale.ROOT), nome);
    }

    private JwtDecoder decoder() {
        if (decoder == null) {
            NimbusJwtDecoder novo = NimbusJwtDecoder.withJwkSetUri(CHAVES_DO_GOOGLE).build();
            novo.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                            iss -> iss != null && EMISSORES.contains(iss.toString())),
                    new JwtClaimValidator<Object>(JwtClaimNames.AUD,
                            aud -> (aud instanceof Collection<?> lista && lista.contains(clientId))
                                    || clientId.equals(aud))
            ));
            decoder = novo;
        }
        return decoder;
    }
}
