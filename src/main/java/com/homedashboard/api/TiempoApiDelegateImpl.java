package com.homedashboard.api;

import com.homedashboard.dtos.ApiResponseTiempo;
import com.homedashboard.services.ITiempoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Implementa el contrato de /api/tiempo. */
@Service
@RequiredArgsConstructor
public class TiempoApiDelegateImpl implements TiempoApiDelegate {

    private final ITiempoService tiempoService;

    @Override
    public ResponseEntity<ApiResponseTiempo> getTiempo() {
        return ResponseEntity.ok(new ApiResponseTiempo()
                .success(true)
                .message("Operación exitosa")
                .data(tiempoService.tiempo()));
    }
}
