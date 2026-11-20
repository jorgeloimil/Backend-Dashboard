package com.homedashboard.mappers;

import com.homedashboard.modelo.ArticuloCompra;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Entidad -> DTO del contrato. Las enumeraciones se mapean por nombre. */
@Mapper(componentModel = "spring")
public interface ArticuloCompraMapper {

    com.homedashboard.dtos.ArticuloCompra toDto(ArticuloCompra articulo);

    com.homedashboard.modelo.CategoriaCompra toModelo(com.homedashboard.dtos.CategoriaCompra categoria);

    default OffsetDateTime toOffsetDateTime(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }
}
