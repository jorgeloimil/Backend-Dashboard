package com.homedashboard.commons.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de texto. */
public final class Textos {

    private Textos() {
    }

    /** Minúsculas, sin tildes y con los espacios normalizados: "  Plátanos  de Canarias" -> "platanos de canarias". */
    public static String clave(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Recorta y colapsa espacios; null o en blanco -> null. */
    public static String limpio(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim().replaceAll("\\s+", " ");
    }
}
