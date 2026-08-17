package com.pipoe.pipoeapi.dominio.pasos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.pasos.entities.Paso;

public interface PasoRepository extends JpaRepository<Paso, Long> {
    /** El producto tiene orden 99, así que queda último sin lógica extra. */
    List<Paso> findByFaseOrderByOrdenAsc(Fase fase);

    long countByFaseAndEsProductoFalse(Fase fase);
}
