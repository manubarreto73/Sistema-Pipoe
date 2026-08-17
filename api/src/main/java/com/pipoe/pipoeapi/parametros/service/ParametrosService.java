package com.pipoe.pipoeapi.parametros.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.parametros.entities.Parametros;
import com.pipoe.pipoeapi.parametros.repositories.ParametrosRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParametrosService {

    private final ParametrosRepository parametrosRepository;

    private static final long CONFIGURACION_ID = 1L;

    public Parametros getById(Long id) {
        return parametrosRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Parametros no encontrados con id: " + id));
    }

    public Parametros getConfiguracion() {
        return getById(CONFIGURACION_ID);
    }

    @Transactional
    public Parametros createDefault() {
        Parametros parametros = new Parametros();
        parametros.setMaxProyectosPorUsuario(5);
        parametros.setMaxColaboradoresPorProyecto(10);
        return parametrosRepository.save(parametros);
    }

    @Transactional
    public Parametros actualizarConfiguracion(Parametros data) {
        return update(CONFIGURACION_ID, data);
    }

    @Transactional
    public Parametros update(Long id, Parametros data) {
        Parametros parametros = getById(id);
        parametros.setMaxProyectosPorUsuario(data.getMaxProyectosPorUsuario());
        parametros.setMaxColaboradoresPorProyecto(data.getMaxColaboradoresPorProyecto());
        return parametrosRepository.save(parametros);
    }
}
