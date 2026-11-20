package com.homedashboard.services;

import com.homedashboard.dtos.ArticuloCompra;
import com.homedashboard.dtos.ArticuloCompraRequest;
import com.homedashboard.dtos.ArticuloCompraUpdate;

import java.util.List;

public interface ICompraService {

    /** Pendientes primero (por pasillo y nombre); después los comprados, lo último primero. */
    List<ArticuloCompra> listar();

    ArticuloCompra anadir(ArticuloCompraRequest request);

    ArticuloCompra actualizar(Long id, ArticuloCompraUpdate cambios);

    void eliminar(Long id);

    int eliminarComprados();
}
