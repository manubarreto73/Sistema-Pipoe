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
 * El editor guarda solo cada dos segundos y medio de inactividad, y **cada guardado escribe una
 * copia completa del documento**. Una sesión de escritura larga deja cientos de filas casi
 * idénticas. Sin poda, la tabla crece sin techo: no es un ataque, es aritmética, y con usuarios
 * reales es el problema de infraestructura más probable del primer año.
 *
 * El criterio conserva lo que sirve y tira lo que no:
 *
 * - **Los últimos días completos**, con todos los guardados. Es la ventana en la que alguien
 *   mira "qué cambió recién" o quiere deshacer algo.
 * - **De ahí para atrás, un guardado por autor y por día.** Alcanza para responder "quién
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
            log.info("Poda del historial: {} versiones intermedias borradas de antes de {}",
                borradas, corte);
    }
}
