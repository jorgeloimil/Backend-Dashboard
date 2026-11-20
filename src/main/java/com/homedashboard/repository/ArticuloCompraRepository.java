package com.homedashboard.repository;

import com.homedashboard.modelo.ArticuloCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ArticuloCompraRepository extends JpaRepository<ArticuloCompra, Long> {

    /** El artículo con esa clave, el pendiente antes que el comprado. */
    Optional<ArticuloCompra> findFirstByNombreClaveOrderByCompradoAscIdDesc(String nombreClave);

    @Modifying
    @Query("DELETE FROM ArticuloCompra a WHERE a.comprado = true")
    int deleteComprados();
}
