package com.pipoe.pipoeapi.dominio.pasos.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.pasos.dtos.PasoCatalogoResponse;
import com.pipoe.pipoeapi.dominio.pasos.dtos.request.ActualizarPasoRequest;
import com.pipoe.pipoeapi.dominio.pasos.entities.Paso;
import com.pipoe.pipoeapi.dominio.pasos.repositories.PasoRepository;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Mantenimiento del catálogo: la explicación y el ejemplo de cada paso. Sólo para el ADMIN. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasoCatalogoService {

    private final PasoRepository pasoRepository;

    public List<PasoCatalogoResponse> listar() {
        return pasoRepository.findAll().stream()
            .sorted(Comparator
                .comparingInt((Paso paso) -> paso.getFase().getOrden())
                .thenComparingInt(Paso::getOrden))
            .map(PasoCatalogoResponse::from)
            .toList();
    }

    @Transactional
    public PasoCatalogoResponse actualizar(Long pasoId, ActualizarPasoRequest request) {
        Paso paso = pasoRepository.findById(pasoId)
            .orElseThrow(() -> new ResourceNotFoundException("Paso no encontrado con id: " + pasoId));

        paso.setExplicacion(request.getExplicacion());
        paso.setEjemplo(request.getEjemplo());
        paso.setTituloCorto(request.getTituloCorto());

        return PasoCatalogoResponse.from(pasoRepository.save(paso));
    }
}
