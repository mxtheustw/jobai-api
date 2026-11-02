package com.vagas.contas;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rotas internas (coletor e match) e as regras do plano Grátis no feed. */
class VagasEPlanoTest extends TesteBase {

    private Map<String, Object> vaga(String fonte, String idFonte) {
        Map<String, Object> v = new HashMap<>();
        v.put("fonte", fonte);
        v.put("id_fonte", idFonte);
        v.put("titulo", "Estágio em Dados " + idFonte);
        v.put("empresa", "Empresa Teste");
        v.put("cidade", "São Paulo");
        v.put("uf", "SP");
        v.put("modelo", "hibrido");
        v.put("senioridade", "estagio");
        v.put("ferramentas", List.of("SQL", "Excel"));
        v.put("url", "https://exemplo.com/vaga/" + idFonte);
        return v;
    }

    private Map<String, Object> match(String usuarioId, String fonte, String idFonte, int percentual) {
        Map<String, Object> m = new HashMap<>();
        m.put("usuarioId", usuarioId);
        m.put("fonte", fonte);
        m.put("id_fonte", idFonte);
        m.put("percentual", percentual);
        m.put("por_que_combina", "Pede Excel e você tem.");
        m.put("o_que_falta", List.of(Map.of("item", "Power BI", "tipo", "habilidade", "impacto", 18)));
        return m;
    }

    @Test
    void rotasInternasExigemAChave() throws Exception {
        mvc.perform(post("/interno/vagas").contentType(MediaType.APPLICATION_JSON).content("{\"vagas\":[]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/interno/vagas").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Chave-Interna", "chave-errada-chave-errada-chave-errada")
                        .content("{\"vagas\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void coletaCriaEAtualizaSemDuplicar() throws Exception {
        String fonte = "teste-" + UUID.randomUUID();
        Map<String, Object> envio = Map.of("vagas", List.of(vaga(fonte, "1"), vaga(fonte, "2")));

        postInterno("/interno/vagas", envio).andExpect(status().isOk())
                .andExpect(jsonPath("$.novas").value(2));
        postInterno("/interno/vagas", envio).andExpect(status().isOk())
                .andExpect(jsonPath("$.novas").value(0))
                .andExpect(jsonPath("$.atualizadas").value(2));
    }

    @Test
    void vagaSemUrlERecusadaSozinha() throws Exception {
        String fonte = "teste-" + UUID.randomUUID();
        Map<String, Object> semUrl = vaga(fonte, "2");
        semUrl.remove("url");

        postInterno("/interno/vagas", Map.of("vagas", List.of(vaga(fonte, "1"), semUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novas").value(1))
                .andExpect(jsonPath("$.recusadas.length()").value(1))
                .andExpect(jsonPath("$.recusadas[0].indice").value(1));
    }

    @Test
    void planoGratisVe3VagasPorSemanaSemOQueFalta() throws Exception {
        String resposta = cadastrar(emailNovo());
        String token = ler(resposta, "$.accessToken");
        String usuarioId = ler(resposta, "$.usuario.id");

        // 5 vagas com match: 95, 85, 70, 60, 50
        String fonte = "teste-" + UUID.randomUUID();
        int[] percentuais = {95, 85, 70, 60, 50};
        List<Map<String, Object>> vagas = new ArrayList<>();
        List<Map<String, Object>> matches = new ArrayList<>();
        for (int i = 0; i < percentuais.length; i++) {
            vagas.add(vaga(fonte, String.valueOf(i)));
            matches.add(match(usuarioId, fonte, String.valueOf(i), percentuais[i]));
        }
        postInterno("/interno/vagas", Map.of("vagas", vagas)).andExpect(status().isOk());
        postInterno("/interno/matches", Map.of("matches", matches)).andExpect(status().isOk());

        // Grátis: só as 3 melhores da semana, sem "o que falta"
        getComToken("/me/vagas", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].match.percentual").value(95))
                .andExpect(jsonPath("$[0].match.destaque").value(true))
                .andExpect(jsonPath("$[2].match.percentual").value(70))
                .andExpect(jsonPath("$[2].match.destaque").value(false))
                .andExpect(jsonPath("$[0].match.oQueFaltaBloqueado").value(true))
                .andExpect(jsonPath("$[0].match.oQueFalta.length()").value(0));

        // Abrir de novo não libera mais vagas na mesma semana
        getComToken("/me/vagas", token).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void naoDaParaAbrirVagaQueNaoFoiLiberada() throws Exception {
        String resposta = cadastrar(emailNovo());
        String token = ler(resposta, "$.accessToken");

        getComToken("/me/vagas/" + UUID.randomUUID(), token).andExpect(status().isNotFound());
    }
}
