package com.homedashboard.services.impl;

import java.util.Map;

/** Códigos WMO del estado del cielo (los que usa Open-Meteo), en español. */
final class CodigosTiempo {

    private static final Map<Integer, String> DESCRIPCIONES = Map.ofEntries(
            Map.entry(0, "Despejado"),
            Map.entry(1, "Casi despejado"),
            Map.entry(2, "Parcialmente nublado"),
            Map.entry(3, "Nublado"),
            Map.entry(45, "Niebla"),
            Map.entry(48, "Niebla con escarcha"),
            Map.entry(51, "Llovizna débil"),
            Map.entry(53, "Llovizna"),
            Map.entry(55, "Llovizna intensa"),
            Map.entry(56, "Llovizna helada"),
            Map.entry(57, "Llovizna helada intensa"),
            Map.entry(61, "Lluvia débil"),
            Map.entry(63, "Lluvia"),
            Map.entry(65, "Lluvia intensa"),
            Map.entry(66, "Lluvia helada"),
            Map.entry(67, "Lluvia helada intensa"),
            Map.entry(71, "Nieve débil"),
            Map.entry(73, "Nieve"),
            Map.entry(75, "Nieve intensa"),
            Map.entry(77, "Granizo fino"),
            Map.entry(80, "Chubascos débiles"),
            Map.entry(81, "Chubascos"),
            Map.entry(82, "Chubascos fuertes"),
            Map.entry(85, "Chubascos de nieve"),
            Map.entry(86, "Chubascos de nieve fuertes"),
            Map.entry(95, "Tormenta"),
            Map.entry(96, "Tormenta con granizo"),
            Map.entry(99, "Tormenta con granizo fuerte"));

    private CodigosTiempo() {
    }

    static String descripcion(int codigo) {
        return DESCRIPCIONES.getOrDefault(codigo, "Desconocido");
    }
}
