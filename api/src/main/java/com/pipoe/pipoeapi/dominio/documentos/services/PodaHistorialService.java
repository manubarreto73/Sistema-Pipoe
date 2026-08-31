package com.pipoe.pipoeapi.dominio.documentos.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.documentos.repositories.DocumentoVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Poda del historial de documentos.
 *
 * La primera línea de defensa ya no es ésta, sino la fusión por sesión de escritura de
 * DocumentoService: los guardados seguidos de una misma persona actualizan una fila en vez de
 * agregar otra, y una tarde de trabajo deja una o dos entradas en lugar de cientos.
 *
 * Esto queda como piso de largo plazo. Un proyecto vivo durante años igual acumula una fila por
 * sesión y por documento, y a partir de cierta antigüedad ese detalle no le importa a nadie:
 *
 * - **Los últimos días completos**, con todas las sesiones. Es la ventana en la que alguien
 *   mira "qué cambió recién" o quiere deshacer algo.
 * - **De ahí para atrás, una sesión por autor y por día.** Alcanza para responder "quién
 *   escribió esto y cuándo", que es para lo que existe la trazabilidad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PodaHistorialService {

    private final DocumentoVersionRepository versionRepository;

    @Value("${app.historial.dias-completos:3}")
    private int diasCompletos;

    /** De madrugada: es cuando menos molesta que la base esté ocupada un rato. */
    @Scheduled(cron = "${app.historial.cron:0 30 4 * * *}")
    @Transactional
    public void podar() {
        LocalDateTime corte = LocalDateTime.now().minusDays(diasCompletos);

        int borradas = versionRepository.podarAnterioresA(corte);

        if (borradas > 0)
            log.info("Poda del historial: {} sesiones intermedias borradas de antes de {}",
                borradas, corte);
    }
}
