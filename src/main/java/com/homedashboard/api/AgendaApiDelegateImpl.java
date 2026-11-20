package com.homedashboard.api;

import com.homedashboard.dtos.ApiResponseAgenda;
import com.homedashboard.services.IAgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Implementa el contrato de /api/agenda. */
@Service
@RequiredArgsConstructor
public class AgendaApiDelegateImpl implements AgendaApiDelegate {

    private final IAgendaService agendaService;

    @Override
    public ResponseEntity<ApiResponseAgenda> getAgenda(Integer dias) {
        return ResponseEntity.ok(new ApiResponseAgenda()
                .success(true)
                .message("Operación exitosa")
                .data(agendaService.agenda(dias == null ? 7 : dias)));
    }
}
