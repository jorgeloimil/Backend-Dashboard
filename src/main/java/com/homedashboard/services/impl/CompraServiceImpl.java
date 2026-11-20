package com.homedashboard.services.impl;

import com.homedashboard.commons.utils.Textos;
import com.homedashboard.dtos.ArticuloCompraRequest;
import com.homedashboard.dtos.ArticuloCompraUpdate;
import com.homedashboard.exceptions.RecursoNoEncontradoException;
import com.homedashboard.mappers.ArticuloCompraMapper;
import com.homedashboard.modelo.ArticuloCompra;
import com.homedashboard.modelo.CategoriaCompra;
import com.homedashboard.repository.ArticuloCompraRepository;
import com.homedashboard.services.ICompraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements ICompraService {

    /** Pendientes por pasillo y nombre; comprados al final, el más reciente primero. */
    private static final Comparator<ArticuloCompra> ORDEN_LISTA = Comparator
            .comparing(ArticuloCompra::isComprado)
            .thenComparing(a -> a.isComprado() ? 0 : a.getCategoria().ordinal())
            .thenComparing(a -> a.isComprado() ? -a.getCompradoEn().toEpochMilli() : 0L)
            .thenComparing(ArticuloCompra::getNombreClave);

    private final ArticuloCompraRepository repository;
    private final ArticuloCompraMapper mapper;
    private final ClasificadorCompra clasificador;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<com.homedashboard.dtos.ArticuloCompra> listar() {
        return repository.findAll().stream()
                .sorted(ORDEN_LISTA)
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public com.homedashboard.dtos.ArticuloCompra anadir(ArticuloCompraRequest request) {
        String nombre = Textos.limpio(request.getNombre());
        String cantidad = Textos.limpio(request.getCantidad());
        Instant ahora = clock.instant();

        ArticuloCompra articulo = repository.findFirstByNombreClaveOrderByCompradoAscIdDesc(Textos.clave(nombre))
                .map(existente -> {
                    // Ya estaba: vuelve a la lista (si se compró) con la cantidad nueva, si la hay
                    existente.marcarComprado(false, ahora);
                    if (cantidad != null) {
                        existente.setCantidad(cantidad);
                    }
                    if (request.getCategoria() != null) {
                        existente.setCategoria(mapper.toModelo(request.getCategoria()));
                    }
                    return existente;
                })
                .orElseGet(() -> nuevo(nombre, cantidad, request.getCategoria(), ahora));

        ArticuloCompra guardado = repository.save(articulo);
        log.info("Compra: '{}' en la lista (id {})", guardado.getNombre(), guardado.getId());
        return mapper.toDto(guardado);
    }

    private ArticuloCompra nuevo(String nombre, String cantidad, com.homedashboard.dtos.CategoriaCompra categoria, Instant ahora) {
        ArticuloCompra articulo = new ArticuloCompra();
        articulo.cambiarNombre(nombre);
        articulo.setCantidad(cantidad);
        articulo.setCategoria(categoria != null ? mapper.toModelo(categoria) : clasificador.clasificar(nombre));
        articulo.setCreadoEn(ahora);
        return articulo;
    }

    @Override
    @Transactional
    public com.homedashboard.dtos.ArticuloCompra actualizar(Long id, ArticuloCompraUpdate cambios) {
        ArticuloCompra articulo = buscar(id);
        if (cambios.getNombre() != null) {
            articulo.cambiarNombre(Textos.limpio(cambios.getNombre()));
        }
        if (cambios.getCantidad() != null) {
            articulo.setCantidad(Textos.limpio(cambios.getCantidad()));
        }
        if (cambios.getCategoria() != null) {
            CategoriaCompra categoria = mapper.toModelo(cambios.getCategoria());
            articulo.setCategoria(categoria);
        }
        if (cambios.getComprado() != null && cambios.getComprado() != articulo.isComprado()) {
            articulo.marcarComprado(cambios.getComprado(), clock.instant());
        }
        return mapper.toDto(articulo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    @Override
    @Transactional
    public int eliminarComprados() {
        int borrados = repository.deleteComprados();
        log.info("Compra: {} artículos comprados quitados de la lista", borrados);
        return borrados;
    }

    private ArticuloCompra buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ningún artículo con id " + id));
    }
}
