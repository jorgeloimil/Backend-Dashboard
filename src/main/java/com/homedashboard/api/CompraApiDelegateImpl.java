package com.homedashboard.api;

import com.homedashboard.dtos.ApiResponseArticuloCompra;
import com.homedashboard.dtos.ApiResponseArticuloCompraList;
import com.homedashboard.dtos.ArticuloCompraRequest;
import com.homedashboard.dtos.ArticuloCompraUpdate;
import com.homedashboard.services.ICompraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Implementa el contrato de /api/compra. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompraApiDelegateImpl implements CompraApiDelegate {

    private static final String MENSAJE_OK = "Operación exitosa";

    private final ICompraService compraService;

    @Override
    public ResponseEntity<ApiResponseArticuloCompraList> listArticulosCompra() {
        return ResponseEntity.ok(new ApiResponseArticuloCompraList()
                .success(true)
                .message(MENSAJE_OK)
                .data(compraService.listar()));
    }

    @Override
    public ResponseEntity<ApiResponseArticuloCompra> addArticuloCompra(ArticuloCompraRequest request) {
        log.debug("POST /api/compra/articulos - {}", request.getNombre());
        return ResponseEntity.ok(new ApiResponseArticuloCompra()
                .success(true)
                .message(MENSAJE_OK)
                .data(compraService.anadir(request)));
    }

    @Override
    public ResponseEntity<ApiResponseArticuloCompra> updateArticuloCompra(Long id, ArticuloCompraUpdate cambios) {
        log.debug("PATCH /api/compra/articulos/{}", id);
        return ResponseEntity.ok(new ApiResponseArticuloCompra()
                .success(true)
                .message(MENSAJE_OK)
                .data(compraService.actualizar(id, cambios)));
    }

    @Override
    public ResponseEntity<Void> deleteArticuloCompra(Long id) {
        log.debug("DELETE /api/compra/articulos/{}", id);
        compraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteArticulosComprados() {
        compraService.eliminarComprados();
        return ResponseEntity.noContent().build();
    }
}
