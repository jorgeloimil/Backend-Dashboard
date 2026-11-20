package com.homedashboard.services.agenda;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RecurrenceId;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lee un calendario iCal (RFC 5545) y devuelve las ocurrencias que caen en un rango de
 * días, con las repeticiones (RRULE, EXDATE) y las ocurrencias modificadas (RECURRENCE-ID)
 * resueltas. Las horas "flotantes" y los días completos se interpretan en la zona de casa.
 */
@org.springframework.stereotype.Component
public class LectorICal {

    /** Ocurrencias que solapan con [desde, hastaExclusivo), ordenadas por inicio. */
    public List<EventoLeido> leer(String ics, LocalDate desde, LocalDate hastaExclusivo, ZoneId zona) {
        Calendar calendario = parsear(ics);
        List<VEvent> eventos = calendario.getComponents(Component.VEVENT);

        // Ocurrencias de una serie que se modificaron: salen como su propio VEVENT con
        // RECURRENCE-ID, y la ocurrencia original de la serie no se debe pintar
        Map<String, Set<Instant>> modificadas = new HashMap<>();
        for (VEvent evento : eventos) {
            recurrenceId(evento).ifPresent(id -> modificadas
                    .computeIfAbsent(uid(evento), k -> new HashSet<>())
                    .add(instante(id, zona)));
        }

        Instant inicioRango = desde.atStartOfDay(zona).toInstant();
        Instant finRango = hastaExclusivo.atStartOfDay(zona).toInstant();
        List<EventoLeido> resultado = new ArrayList<>();
        for (VEvent evento : eventos) {
            if (cancelado(evento)) {
                continue;
            }
            Optional<DtStart<Temporal>> inicio = evento.getStartDate();
            if (inicio.isEmpty()) {
                continue;
            }
            boolean todoElDia = inicio.get().getDate() instanceof LocalDate;
            boolean esSerie = recurrenceId(evento).isEmpty() && evento.getProperty(Property.RRULE).isPresent();
            Set<Instant> excluidas = esSerie ? modificadas.getOrDefault(uid(evento), Set.of()) : Set.of();

            for (Period<Temporal> ocurrencia : ocurrencias(evento, inicio.get().getDate(), desde, hastaExclusivo, zona)) {
                Instant empieza = instante(ocurrencia.getStart(), zona);
                Instant acaba = instante(ocurrencia.getEnd(), zona);
                if (excluidas.contains(empieza)) {
                    continue;
                }
                // Un evento sin duración (solo DTSTART) dura 0: cuenta si empieza dentro
                boolean solapa = acaba.isAfter(inicioRango) || (acaba.equals(empieza) && !empieza.isBefore(inicioRango));
                if (solapa && empieza.isBefore(finRango)) {
                    resultado.add(new EventoLeido(texto(evento, Property.SUMMARY).orElse("(Sin título)"),
                            empieza, acaba, todoElDia, texto(evento, Property.LOCATION).orElse(null)));
                }
            }
        }
        resultado.sort(Comparator.comparing(EventoLeido::inicio).thenComparing(EventoLeido::titulo));
        return resultado;
    }

    private static Calendar parsear(String ics) {
        try {
            return new CalendarBuilder().build(new StringReader(ics));
        } catch (IOException | ParserException e) {
            throw new IllegalArgumentException("El calendario no es un iCal válido: " + e.getMessage(), e);
        }
    }

    /**
     * Ocurrencias de un evento en el rango. ical4j compara fechas del mismo tipo que el
     * DTSTART, así que el rango se construye con ese tipo (día, hora local o con zona).
     * Se amplía un día por cada lado: los eventos que empiezan antes y siguen dentro
     * también cuentan, y el filtro fino se hace después con instantes.
     */
    private static Set<Period<Temporal>> ocurrencias(VEvent evento, Temporal dtStart, LocalDate desde, LocalDate hasta, ZoneId zona) {
        LocalDate antes = desde.minusDays(1);
        LocalDate despues = hasta.plusDays(1);
        Period<? extends Temporal> rango;
        if (dtStart instanceof LocalDate) {
            rango = new Period<>(antes, despues);
        } else if (dtStart instanceof LocalDateTime) {
            rango = new Period<>(antes.atStartOfDay(), despues.atStartOfDay());
        } else if (dtStart instanceof Instant) {
            rango = new Period<>(antes.atStartOfDay(zona).toInstant(), despues.atStartOfDay(zona).toInstant());
        } else {
            rango = new Period<>(antes.atStartOfDay(zona), despues.atStartOfDay(zona));
        }
        return evento.calculateRecurrenceSet(rango);
    }

    private static Instant instante(Temporal temporal, ZoneId zona) {
        if (temporal instanceof LocalDate dia) {
            return dia.atStartOfDay(zona).toInstant();
        }
        if (temporal instanceof LocalDateTime hora) {
            return hora.atZone(zona).toInstant();
        }
        return Instant.from(temporal);
    }

    private static Instant instante(RecurrenceId<Temporal> id, ZoneId zona) {
        return instante(id.getDate(), zona);
    }

    private static Optional<RecurrenceId<Temporal>> recurrenceId(VEvent evento) {
        return evento.getProperty(Property.RECURRENCE_ID);
    }

    private static String uid(VEvent evento) {
        return texto(evento, Property.UID).orElse("");
    }

    private static boolean cancelado(VEvent evento) {
        return texto(evento, Property.STATUS).map("CANCELLED"::equalsIgnoreCase).orElse(false);
    }

    private static Optional<String> texto(VEvent evento, String propiedad) {
        return evento.<Property>getProperty(propiedad)
                .map(Property::getValue)
                .map(String::strip)
                .filter(valor -> !valor.isEmpty());
    }
}
