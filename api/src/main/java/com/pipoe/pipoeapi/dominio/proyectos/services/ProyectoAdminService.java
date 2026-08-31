package com.pipoe.pipoeapi.dominio.proyectos.services;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.colaboradores.repositories.ColaboradorRepository;
import com.pipoe.pipoeapi.dominio.documentos.repositories.DocumentoRepository;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.proyectos.dtos.AdminProyectoResponse;
import com.pipoe.pipoeapi.dominio.proyectos.dtos.EstadoFase;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.proyectos.repositories.ProyectoRepository;

import lombok.RequiredArgsConstructor;

/**
 * El listado de todos los proyectos, para la administradora.
 *
 * Aparte de ProyectoService a propósito: aquél responde "los proyectos de esta persona" y es el
 * que guarda la regla de propiedad; éste mira el sistema entero y necesita datos de otros dos
 * dominios (documentos y colaboradores) que ProyectoService no tiene por qué conocer.
 *
 * Depende de repositorios y no de servicios: documentos ya depende de proyectos, y pedirle el
 * servicio cerraría el ciclo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProyectoAdminService {

    private final ProyectoRepository proyectoRepository;
    private final DocumentoRepository documentoRepository;
    private final ColaboradorRepository colaboradorRepository;

    /** `terminado` nulo trae todos; true, sólo los cerrados; false, los que siguen en curso. */
    public Page<AdminProyectoResponse> listar(String texto, Boolean terminado, Pageable pageable) {
        Page<Proyecto> pagina = proyectoRepository.buscarParaAdmin(
            patron(texto), terminado, Fase.values().length, pageable
        );

        List<Long> ids = pagina.getContent().stream().map(Proyecto::getId).toList();

        // Dos consultas para toda la página, en vez de dos por proyecto. Con la lista vacía no se
        // preguntan: un IN () no es SQL válido y Hibernate no siempre lo evita.
        Map<Long, Map<Fase, EstadoFase>> avance = ids.isEmpty() ? Map.of() : avancePorProyecto(ids);
        Map<Long, Integer> colaboradores = ids.isEmpty() ? Map.of() : colaboradoresPorProyecto(ids);

        return pagina.map(proyecto -> {
            Map<Fase, EstadoFase> fases = avance.getOrDefault(proyecto.getId(), sinEmpezar());

            return AdminProyectoResponse.builder()
                .id(proyecto.getId())
                .nombre(proyecto.getNombre())
                .codigo(proyecto.getCodigo())
                .duenioId(proyecto.getUsuario().getId())
                .duenio(proyecto.getUsuario().getNombreCompleto())
                .colaboradores(colaboradores.getOrDefault(proyecto.getId(), 0))
                .fases(fases)
                .terminado(fases.values().stream().allMatch(estado -> estado == EstadoFase.COMPLETA))
                .build();
        });
    }

    /**
     * El estado de las 5 fases de cada proyecto.
     *
     * La fase está COMPLETA cuando su producto está completado —es el entregable que la cierra—
     * y EN_PROGRESO cuando hay algún paso completado pero todavía no el producto.
     */
    private Map<Long, Map<Fase, EstadoFase>> avancePorProyecto(List<Long> ids) {
        Map<Long, Map<Fase, EstadoFase>> porProyecto = new HashMap<>();

        for (Object[] fila : documentoRepository.completadosDe(ids)) {
            Long proyectoId = (Long) fila[0];
            Fase fase = (Fase) fila[1];
            boolean esProducto = (Boolean) fila[2];

            Map<Fase, EstadoFase> fases =
                porProyecto.computeIfAbsent(proyectoId, id -> sinEmpezar());

            // El producto manda: un paso completado no puede bajar a EN_PROGRESO una fase que
            // ya se cerró, y el orden en que llegan las filas no está garantizado.
            if (esProducto) fases.put(fase, EstadoFase.COMPLETA);
            else if (fases.get(fase) != EstadoFase.COMPLETA) fases.put(fase, EstadoFase.EN_PROGRESO);
        }

        return porProyecto;
    }

    private Map<Long, Integer> colaboradoresPorProyecto(List<Long> ids) {
        Map<Long, Integer> conteo = new HashMap<>();

        for (Object[] fila : colaboradorRepository.contarActivosPorProyecto(ids))
            conteo.put((Long) fila[0], ((Number) fila[1]).intValue());

        return conteo;
    }

    /** Las 5 fases en cero. EnumMap y no HashMap: el orden de iteración es el de las fases. */
    private Map<Fase, EstadoFase> sinEmpezar() {
        Map<Fase, EstadoFase> fases = new EnumMap<>(Fase.class);

        for (Fase fase : Fase.values()) fases.put(fase, EstadoFase.SIN_EMPEZAR);

        return fases;
    }

    /** Mismo criterio que en solicitudes: los comodines que escriba la persona se descartan. */
    private String patron(String texto) {
        if (texto == null || texto.isBlank()) return null;

        String limpio = texto.trim().toLowerCase(Locale.ROOT).replaceAll("[%_\\\\]", "");

        return limpio.isEmpty() ? null : "%" + limpio + "%";
    }
}
