package com.homedashboard.services;

import com.homedashboard.dtos.Tiempo;

public interface ITiempoService {

    /** Tiempo actual y previsión; de la caché si es reciente. */
    Tiempo tiempo();
}
