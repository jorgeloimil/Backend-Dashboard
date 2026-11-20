package com.homedashboard.services.impl;

import com.homedashboard.commons.utils.Textos;
import com.homedashboard.modelo.CategoriaCompra;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adivina el pasillo de un artículo por palabras clave, para que apuntar algo desde la
 * tablet sea escribir el nombre y ya. Si no reconoce nada, OTROS; siempre se puede cambiar.
 */
@Component
public class ClasificadorCompra {

    private static final Map<CategoriaCompra, List<String>> PALABRAS = new LinkedHashMap<>();

    static {
        PALABRAS.put(CategoriaCompra.LIMPIEZA, List.of(
                "detergente", "lejia", "suavizante", "lavavajillas", "fregasuelos", "estropajo",
                "bayeta", "papel de cocina", "bolsas de basura", "limpiador", "friegaplatos"));
        PALABRAS.put(CategoriaCompra.HIGIENE, List.of(
                "champu", "gel", "desodorante", "pasta de dientes", "dentifrico", "cepillo de dientes",
                "papel higienico", "compresas", "tampones", "cuchillas", "crema hidratante", "colutorio", "panales", "toallitas"));
        PALABRAS.put(CategoriaCompra.CONGELADOS, List.of("congelado", "congelada", "helado", "pizza"));
        PALABRAS.put(CategoriaCompra.BEBIDAS, List.of(
                "agua", "cerveza", "vino", "refresco", "coca", "zumo", "cafe", "te", "infusion", "tonica", "gaseosa"));
        PALABRAS.put(CategoriaCompra.LACTEOS_HUEVOS, List.of(
                "leche", "yogur", "queso", "huevo", "mantequilla", "nata", "kefir", "requeson", "bebida de avena"));
        PALABRAS.put(CategoriaCompra.CARNE_PESCADO, List.of(
                "pollo", "pavo", "ternera", "cerdo", "carne", "filete", "hamburguesa", "salchicha", "chorizo",
                "jamon", "lomo", "pechuga", "pescado", "merluza", "salmon", "atun fresco", "bacalao", "gambas",
                "langostinos", "sepia", "calamar", "mejillones", "lubina", "dorada"));
        PALABRAS.put(CategoriaCompra.PANADERIA, List.of("pan", "baguette", "barra", "picos", "tostadas", "bolleria", "croissant"));
        PALABRAS.put(CategoriaCompra.DESPENSA, List.of(
                "arroz", "pasta", "macarrones", "espagueti", "harina", "azucar", "sal", "aceite", "vinagre",
                "legumbre", "lentejas", "garbanzos", "alubias", "atun", "conserva", "tomate frito", "cereales",
                "avena", "galletas", "chocolate", "especias", "caldo", "miel", "mermelada", "frutos secos"));
        PALABRAS.put(CategoriaCompra.FRUTA_VERDURA, List.of(
                "manzana", "platano", "naranja", "mandarina", "pera", "uva", "fresa", "kiwi", "limon", "melon",
                "sandia", "pina", "aguacate", "tomate", "lechuga", "cebolla", "ajo", "patata", "zanahoria",
                "pimiento", "calabacin", "berenjena", "pepino", "brocoli", "espinaca", "champinon", "seta",
                "fruta", "verdura", "ensalada", "puerro", "calabaza", "judias verdes"));
    }

    /** Por cada pasillo, una expresión que busca sus palabras enteras, también en plural. */
    private static final Map<CategoriaCompra, Pattern> PATRONES = new LinkedHashMap<>();

    static {
        PALABRAS.forEach((categoria, palabras) -> PATRONES.put(categoria, Pattern.compile(
                palabras.stream().map(Pattern::quote).collect(Collectors.joining("|", "(?:^| )(?:", ")(?:s|es)?(?: |$)")))));
    }

    public CategoriaCompra clasificar(String nombre) {
        String clave = Textos.clave(nombre);
        // El orden importa: "tomate frito" es despensa antes que verdura
        return PATRONES.entrySet().stream()
                .filter(entrada -> entrada.getValue().matcher(clave).find())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(CategoriaCompra.OTROS);
    }
}
