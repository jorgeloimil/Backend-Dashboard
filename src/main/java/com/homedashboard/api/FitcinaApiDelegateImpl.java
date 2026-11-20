package com.homedashboard.api;

import com.homedashboard.dtos.ApiResponseFitcinaHoy;
import com.homedashboard.services.IFitcinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Implementa el contrato de /api/fitcina. */
@Service
@RequiredArgsConstructor
public class FitcinaApiDelegateImpl implements FitcinaApiDelegate {

    private final IFitcinaService fitcinaService;

    @Override
    public ResponseEntity<ApiResponseFitcinaHoy> getFitcinaHoy() {
        return ResponseEntity.ok(new ApiResponseFitcinaHoy()
                .success(true)
                .message("Operación exitosa")
                .data(fitcinaService.hoy()));
    }
}
