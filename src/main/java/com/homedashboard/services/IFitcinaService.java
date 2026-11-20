package com.homedashboard.services;

import com.homedashboard.dtos.FitcinaHoy;

public interface IFitcinaService {

    /** Rutinas que tocan hoy y su estado; nunca lanza: si Fitcina falla, lo dice el estado. */
    FitcinaHoy hoy();
}
