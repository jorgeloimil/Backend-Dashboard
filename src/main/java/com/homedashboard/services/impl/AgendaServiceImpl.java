package com.homedashboard.services.impl;

import com.homedashboard.config.PanelProperties;
import com.homedashboard.dtos.Agenda;
import com.homedashboard.dtos.CalendarioAgenda;
import com.homedashboard.dtos.EventoAgenda;
import com.homedashboard.services.IAgendaService;
import com.homedashboard.services.agenda.EventoLeido;
import com.homedashboard.services.agenda.LectorICal;
import com.homedashboard.services.external.DescargadorCalendarios;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Junta los calendarios iCal configurados. Cada calendario se descarga como mucho una
 * vez cada {@code panel.agenda.cache}; si falla, se usa la última copia buena (y se
 * avisa con {@code ok: false}) para que la tablet no se quede sin agenda por un corte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaServiceImpl implements IAgendaService {

    private final PanelProperties properties;
    private final DescargadorCalendarios descargador;
    private final LectorICal lector;
    private final Clock clock;

    /** Última descarga buena de cada calendario, por URL. */
    private final Map<String, Descarga> descargas = new ConcurrentHashMap<>();

    private record Descarga(String ics, Instant cuando) {
    }

    /** Olvida las copias descargadas: la próxima consulta vuelve a pedir los calendarios. */
    public void vaciarCache() {
        descargas.clear();
    }

    @Override
    public Agenda agenda(int dias) {
        ZoneId zona = properties.zona();
        Instant ahora = clock.instant();
        LocalDate hoy = LocalDate.ofInstant(ahora, zona);
        LocalDate finExclusivo = hoy.plusDays(dias);

        List<EventoAgenda> eventos = new ArrayList<>();
        List<CalendarioAgenda> calendarios = new ArrayList<>();
        for (PanelProperties.Calendario calendario : properties.agenda().calendarios()) {
            CalendarioAgenda estado = new CalendarioAgenda()
                    .nombre(calendario.nombre())
                    .color(calendario.color())
                    .ok(true);
            try {
                String ics = ics(calendario, ahora, estado);
                for (EventoLeido evento : lector.leer(ics, hoy, finExclusivo, zona)) {
                    eventos.add(new EventoAgenda()
                            .titulo(evento.titulo())
                            .inicio(evento.inicio().atOffset(ZoneOffset.UTC))
                            .fin(evento.fin().atOffset(ZoneOffset.UTC))
                            .todoElDia(evento.todoElDia())
                            .ubicacion(evento.ubicacion())
                            .calendario(calendario.nombre())
                            .color(calendario.color()));
                }
            } catch (RuntimeException e) {
                log.warn("Agenda: el calendario '{}' no se puede leer: {}", calendario.nombre(), e.getMessage());
                estado.ok(false).error(e.getMessage());
            }
            calendarios.add(estado);
        }

        // Los de todo el día, antes que los que tienen hora ese mismo día
        eventos.sort(Comparator.comparing(EventoAgenda::getInicio)
                .thenComparing(evento -> !evento.getTodoElDia())
                .thenComparing(EventoAgenda::getTitulo));

        return new Agenda()
                .desde(hoy)
                .hasta(finExclusivo.minusDays(1))
                .eventos(eventos)
                .calendarios(calendarios);
    }

    /** El iCal del calendario: de la caché si es reciente; si la descarga falla, la última copia. */
    private String ics(PanelProperties.Calendario calendario, Instant ahora, CalendarioAgenda estado) {
        String clave = calendario.url().toString();
        Descarga anterior = descargas.get(clave);
        if (anterior != null && anterior.cuando().plus(properties.agenda().cache()).isAfter(ahora)) {
            return anterior.ics();
        }
        try {
            String ics = descargador.descargar(calendario.url());
            descargas.put(clave, new Descarga(ics, ahora));
            return ics;
        } catch (RuntimeException e) {
            if (anterior == null) {
                throw e;
            }
            estado.ok(false).error(e.getMessage() + " (se muestra la copia de " + anterior.cuando() + ")");
            return anterior.ics();
        }
    }
}
