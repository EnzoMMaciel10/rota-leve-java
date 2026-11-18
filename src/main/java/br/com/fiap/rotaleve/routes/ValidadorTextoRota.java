package br.com.fiap.rotaleve.routes;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Restringe o resumo ao contrato numérico; conclusões e equipamentos ficam fora do escopo do LLM. */
final class ValidadorTextoRota {
    private static final Pattern NUMERO = Pattern.compile("[0-9]+(?:[.,][0-9]+)?");
    private static final Pattern UNIDADE = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(metros?\\b|m\\b|minutos?\\b|min\\b)");
    private static final Pattern NOTA = Pattern.compile("\\b(?:nota|score)\\b\\D{0,50}?([0-9]+(?:[.,][0-9]+)?)");
    private static final Pattern FORA_DO_ESCOPO = Pattern.compile(
        "\\b(?:segur\\w*|assegur\\w*|confort\\w*|agrad\\w*|garant\\w*|veloc\\w*|eficien\\w*"
        + "|media|porcent\\w*|percent\\w*|caminhou|caminharam|percorreu|realizou"
        + "|elevador\\w*|calcad\\w*|assent\\w*|ramp\\w*|escad\\w*"
        + "|incompativ\\w*|compativ\\w*|acessiv\\w*|acessibilidade|acesso\\w*|recomend\\w*"
        + "|adequad[oa]s?|otim[oa]s?|confiav\\w*|bo[am]s?|bons|alt[oa]s?|baix[oa]s?|facil\\w*|livr\\w*)\\b"
        + "|%|km\\s*/\\s*h|\\bquilometros?\\b|\\bkm\\b|[+-]\\s*[0-9]");

    private ValidadorTextoRota() { }

    static boolean valido(String texto, RotasDtos.Avaliacao avaliacao) {
        if (texto == null || texto.isBlank() || texto.length() > 1500) return false;
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
        if (normalizado.split(" ").length > 80 || FORA_DO_ESCOPO.matcher(normalizado).find()) return false;
        String metros = String.valueOf(avaliacao.rota().caminhadaMetros());
        String minutos = String.valueOf(avaliacao.rota().duracaoSegundos() / 60);
        String score = String.valueOf(avaliacao.score());
        // Valores podem coincidir: score=100 não deve causar falha na construção do conjunto.
        Set<String> permitidos = new java.util.HashSet<>(java.util.List.of(metros, minutos, score, "100"));
        var numeros = NUMERO.matcher(normalizado);
        int quantidade = 0;
        while (numeros.find()) {
            quantidade++;
            if (!permitidos.contains(numeros.group())) return false;
        }
        if (quantidade == 0) return false;
        var unidades = UNIDADE.matcher(normalizado);
        boolean informouDuracao = false;
        while (unidades.find()) {
            boolean tempo = unidades.group(2).startsWith("min");
            if (!unidades.group(1).equals(tempo ? minutos : metros)) return false;
            informouDuracao |= tempo;
        }
        if (informouDuracao && !(normalizado.contains("duracao total") || normalizado.contains("tempo total")
                || normalizado.contains("viagem no total") || normalizado.contains("total da viagem"))) return false;
        var notas = NOTA.matcher(normalizado);
        while (notas.find()) if (!score.equals(notas.group(1))) return false;
        return true;
    }
}
