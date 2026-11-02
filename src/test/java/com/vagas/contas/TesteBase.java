package com.vagas.contas;

import com.jayway.jsonpath.JsonPath;
import com.vagas.contas.email.EnvioEmail;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de todos os testes: liga a API inteira (com o banco "contas_teste") e oferece atalhos
 * para chamar as rotas como o site chamaria.
 *
 * O envio de e-mail é trocado por um "dublê" (mock): nada sai de verdade, e o teste consegue
 * ler o e-mail que teria sido enviado.
 */
@SpringBootTest
@ActiveProfiles("teste")
public abstract class TesteBase {

    protected static final String SENHA = "senhaDeTeste123";
    protected static final String CHAVE_INTERNA = "chave-interna-de-teste-com-mais-de-32-caracteres";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    protected JsonMapper json;

    @MockitoBean
    protected EnvioEmail envioEmail;

    protected MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    /** Um e-mail que nunca se repete, para um teste não atrapalhar o outro. */
    protected static String emailNovo() {
        return "teste-" + UUID.randomUUID() + "@teste.com";
    }

    /** O mesmo perfil do exemplo do README (formato do profile.ts do site). */
    protected Map<String, Object> perfil(String email) {
        return Map.ofEntries(
                Map.entry("nome", "Ana Souza"),
                Map.entry("email", email),
                Map.entry("local", Map.of("cidade", "São Paulo", "uf", "SP")),
                Map.entry("momento", "primeiro-emprego"),
                Map.entry("areas", List.of("Tecnologia")),
                Map.entry("niveis", List.of("estagio", "junior")),
                Map.entry("contratos", List.of("clt")),
                Map.entry("modalidades", List.of("remoto", "hibrido")),
                Map.entry("alcance", "minha-cidade"),
                Map.entry("turnos", List.of("integral")),
                Map.entry("escolaridade", Map.of("nivel", "superior", "status", "cursando")),
                Map.entry("curso", "Análise e Desenvolvimento de Sistemas"),
                Map.entry("experiencias", List.of("projeto")),
                Map.entry("habilidades", List.of("Excel", "Comunicação")),
                Map.entry("idiomas", List.of(Map.of("idioma", "Inglês", "nivel", "basico"))),
                Map.entry("cargosDesejados", List.of("Analista de Dados")),
                Map.entry("cidadesTrabalho", List.of(Map.of("cidade", "Campinas", "uf", "SP"))),
                Map.entry("qualquerCidade", false)
        );
    }

    protected ResultActions postJson(String rota, Object corpo) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }

    protected ResultActions postJson(String rota, Object corpo, String accessToken) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(json.writeValueAsString(corpo)));
    }

    protected ResultActions getComToken(String rota, String accessToken) throws Exception {
        return mvc.perform(get(rota).header("Authorization", "Bearer " + accessToken));
    }

    protected ResultActions deleteComToken(String rota, Object corpo, String accessToken) throws Exception {
        return mvc.perform(delete(rota).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(json.writeValueAsString(corpo)));
    }

    protected ResultActions postInterno(String rota, Object corpo) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON)
                .header("X-Chave-Interna", CHAVE_INTERNA)
                .content(json.writeValueAsString(corpo)));
    }

    /** Cria uma conta nova com e-mail e senha. Devolve o JSON da resposta (com os tokens). */
    protected String cadastrar(String email) throws Exception {
        return postJson("/auth/cadastro", Map.of(
                "perfil", perfil(email),
                "senha", SENHA,
                "aceiteTermos", Map.of("versao", "2026-10"),
                "dispositivo", "WEB"
        )).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    protected ResultActions login(String email, String senha) throws Exception {
        return postJson("/auth/login", Map.of("email", email, "senha", senha, "dispositivo", "WEB"));
    }

    /** Lê um campo de uma resposta JSON. Ex.: ler(resposta, "$.accessToken") */
    protected static String ler(String respostaJson, String caminho) {
        Object valor = JsonPath.read(respostaJson, caminho);
        return valor == null ? null : valor.toString();
    }
}
