package com.vagas.contas.auth;

import com.vagas.contas.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cria os dois "crachás" de login:
 * - accessToken (JWT): curto, prova quem é a pessoa em cada requisição.
 * - refreshToken: longo, guardado no banco (como hash), serve para renovar o accessToken.
 */
@Service
public class TokenService {

    public static final String EMISSOR = "jobai-api-contas";

    private final JwtEncoder jwtEncoder;
    private final SessaoRepository sessaoRepository;
    private final long minutosAcesso;
    private final long diasRefresh;
    private final SecureRandom aleatorio = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder,
                        SessaoRepository sessaoRepository,
                        @Value("${app.jwt.minutos-acesso:15}") long minutosAcesso,
                        @Value("${app.jwt.dias-refresh:30}") long diasRefresh) {
        this.jwtEncoder = jwtEncoder;
        this.sessaoRepository = sessaoRepository;
        this.minutosAcesso = minutosAcesso;
        this.diasRefresh = diasRefresh;
    }

    public record Tokens(String accessToken, String refreshToken, long expiraEmSegundos) {
    }

    public Tokens gerar(Usuario usuario, Dispositivo dispositivo) {
        Instant agora = Instant.now();

        // 1. accessToken (JWT assinado com a chave JWT_SECRET)
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(minutosAcesso, ChronoUnit.MINUTES))
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();

        // 2. refreshToken (texto aleatório; no banco vai só o hash)
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessaoRepository.save(new Sessao(usuario.getId(), hash(refreshToken), dispositivo,
                agora.plus(diasRefresh, ChronoUnit.DAYS)));

        return new Tokens(accessToken, refreshToken, minutosAcesso * 60);
    }

    /** SHA-256 do token. Se alguém roubar o banco, não consegue usar os tokens. */
    public static String hash(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
