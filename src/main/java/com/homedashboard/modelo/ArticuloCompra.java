package com.homedashboard.modelo;

import com.homedashboard.commons.utils.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Un artículo de la lista de la compra de casa. */
@Entity
@Table(name = "articulo_compra")
@Getter
@Setter
@NoArgsConstructor
public class ArticuloCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** {@link Textos#clave} del nombre: detecta duplicados sin mirar mayúsculas ni tildes. */
    @Column(name = "nombre_clave", nullable = false, length = 80)
    private String nombreClave;

    @Column(length = 30)
    private String cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCompra categoria = CategoriaCompra.OTROS;

    @Column(nullable = false)
    private boolean comprado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "comprado_en")
    private Instant compradoEn;

    public void cambiarNombre(String nuevoNombre) {
        this.nombre = nuevoNombre;
        this.nombreClave = Textos.clave(nuevoNombre);
    }

    public void marcarComprado(boolean comprado, Instant ahora) {
        this.comprado = comprado;
        this.compradoEn = comprado ? ahora : null;
    }
}
