package com.homedashboard.services;

import com.homedashboard.dtos.Agenda;

public interface IAgendaService {

    /** Eventos desde hoy (en casa) durante {@code dias} días. */
    Agenda agenda(int dias);
}
