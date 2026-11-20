package com.homedashboard.services.impl;

import com.homedashboard.modelo.CategoriaCompra;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClasificadorCompraTest {

    private final ClasificadorCompra clasificador = new ClasificadorCompra();

    @Test
    void reconocePasillosPorPalabrasClave() {
        assertThat(clasificador.clasificar("Leche semidesnatada")).isEqualTo(CategoriaCompra.LACTEOS_HUEVOS);
        assertThat(clasificador.clasificar("Plátanos")).isEqualTo(CategoriaCompra.FRUTA_VERDURA);
        assertThat(clasificador.clasificar("pechuga de pollo")).isEqualTo(CategoriaCompra.CARNE_PESCADO);
        assertThat(clasificador.clasificar("Pan de molde")).isEqualTo(CategoriaCompra.PANADERIA);
        assertThat(clasificador.clasificar("detergente")).isEqualTo(CategoriaCompra.LIMPIEZA);
        assertThat(clasificador.clasificar("Papel higiénico")).isEqualTo(CategoriaCompra.HIGIENE);
    }

    @Test
    void elOrdenDesempata() {
        assertThat(clasificador.clasificar("tomate frito")).isEqualTo(CategoriaCompra.DESPENSA);
        assertThat(clasificador.clasificar("tomates")).isEqualTo(CategoriaCompra.FRUTA_VERDURA);
        assertThat(clasificador.clasificar("pizza congelada")).isEqualTo(CategoriaCompra.CONGELADOS);
    }

    @Test
    void laPalabraDebeSerEntera() {
        assertThat(clasificador.clasificar("te verde")).isEqualTo(CategoriaCompra.BEBIDAS);
        assertThat(clasificador.clasificar("tomate")).isNotEqualTo(CategoriaCompra.BEBIDAS);
    }

    @Test
    void loDesconocidoEsOtros() {
        assertThat(clasificador.clasificar("Pilas AAA")).isEqualTo(CategoriaCompra.OTROS);
    }
}
