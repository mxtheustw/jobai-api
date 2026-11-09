package com.vagas.contas.email;

import java.util.Locale;

/** O "visual" dos e-mails do JobAI: saudação, mensagem, botão amarelo e assinatura. */
public final class ModeloEmail {

    private ModeloEmail() {
    }

    /** "Oi, Matheus!" (só o primeiro nome, com a primeira letra maiúscula). Sem nome: "Oi!". */
    public static String saudacao(String nome) {
        String primeiro = nome == null ? "" : nome.trim().split("\\s+")[0];
        if (primeiro.isEmpty()) return "Oi!";
        return "Oi, " + primeiro.substring(0, 1).toUpperCase(Locale.of("pt", "BR")) + primeiro.substring(1) + "!";
    }

    /**
     * Monta o HTML do e-mail.
     * @param mensagem  texto principal (pode ter HTML simples, como &lt;b&gt;)
     * @param link      endereço do botão (null = sem botão)
     * @param botao     texto do botão
     * @param rodape    frase pequena depois do botão (null = nenhuma)
     */
    public static String html(String nome, String mensagem, String link, String botao, String rodape) {
        StringBuilder sb = new StringBuilder()
                .append("<div style=\"font-family:Arial,sans-serif;max-width:520px;color:#1D2B22\">")
                .append("<p>").append(escapar(saudacao(nome))).append("</p>")
                .append("<p>").append(mensagem).append("</p>");
        if (link != null) {
            sb.append("<p><a href=\"").append(link).append("\" style=\"display:inline-block;padding:12px 20px;")
                    .append("background:#E8B10E;color:#1D2B22;text-decoration:none;border-radius:6px;font-weight:bold\">")
                    .append(escapar(botao)).append("</a></p>")
                    .append("<p style=\"font-size:13px;color:#555\">Se o botão não funcionar, copie este link:<br>")
                    .append(link).append("</p>");
        }
        if (rodape != null) {
            sb.append("<p style=\"font-size:13px;color:#555\">").append(escapar(rodape)).append("</p>");
        }
        return sb.append("<p>Equipe JobAI</p></div>").toString();
    }

    public static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
