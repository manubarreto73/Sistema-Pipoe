package com.pipoe.pipoeapi.dominio.documentos.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.documentos.dtos.FaseResumenResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.PasoDetalleResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.PasoResumenResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.VersionDiffResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.VersionResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.request.GuardarDocumentoRequest;
import com.pipoe.pipoeapi.dominio.documentos.entities.Documento;
import com.pipoe.pipoeapi.dominio.documentos.entities.DocumentoVersion;
import com.pipoe.pipoeapi.dominio.documentos.entities.EstadoPaso;
import com.pipoe.pipoeapi.dominio.documentos.repositories.DocumentoRepository;
import com.pipoe.pipoeapi.dominio.documentos.repositories.DocumentoVersionRepository;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.pasos.entities.Paso;
import com.pipoe.pipoeapi.dominio.pasos.repositories.PasoRepository;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.exceptions.exceptions.ConflictException;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;
import com.pipoe.pipoeapi.redis.RedisService;
import com.pipoe.pipoeapi.utils.HtmlSanitizer;

import lombok.RequiredArgsConstructor;

/**
 * El trabajo dentro de un proyecto: avanzar en los pasos de las 5 fases.
 *
 * Las fases no son secuenciales entre sí. Dentro de una fase, los pasos tampoco lo son del
 * todo: se puede editar cualquiera en cualquier momento, pero **completar** el paso N exige
 * que el N-1 tenga algo escrito, y completar el producto exige la fase entera terminada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentoService {

    private final PasoRepository pasoRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoVersionRepository versionRepository;
    private final AccesoFaseService accesoFaseService;
    private final ExportacionService exportacionService;
    private final RedisService redisService;

    /** Marca de "hay alguien acá adentro". Vive en Redis y se renueva con un latido. */
    private static final String PRESENCIA = "editando";
    private static final long PRESENCIA_TTL_MINUTOS = 2;
    /** El cliente late cada 25s: pasado el minuto damos por hecho que cerró la pestaña. */
    private static final long PRESENCIA_VENCE_SEGUNDOS = 60;
    private static final int VERSIONES_EN_HISTORIAL = 50;

    // ---------------------------------------------------------------- lecturas

    public List<FaseResumenResponse> resumenFases(Long proyectoId, UserDetails principal) {
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        Map<Long, Documento> documentos = documentosPorPaso(proyecto);

        List<FaseResumenResponse> resumen = new ArrayList<>();

        for (Fase fase : Fase.values()) {
            List<Paso> pasos = pasoRepository.findByFaseOrderByOrdenAsc(fase);

            List<Paso> delDespliegue = pasos.stream().filter(paso -> !paso.isEsProducto()).toList();
            Paso producto = pasos.stream().filter(Paso::isEsProducto).findFirst().orElse(null);

            long completados = delDespliegue.stream()
                .filter(paso -> estado(documentos.get(paso.getId())) == EstadoPaso.COMPLETADO)
                .count();

            boolean productoHabilitado = completados == delDespliegue.size();

            resumen.add(FaseResumenResponse.builder()
                .fase(fase)
                .nombre(fase.getNombre())
                .ideaCentral(fase.getIdeaCentral())
                .orden(fase.getOrden())
                .producto(producto != null ? producto.getTituloCorto() : null)
                .productoCompletado(producto != null
                    && estado(documentos.get(producto.getId())) == EstadoPaso.COMPLETADO)
                .productoHabilitado(productoHabilitado)
                .totalPasos(delDespliegue.size())
                .pasosCompletados((int) completados)
                .nivel(accesoFaseService.nivel(proyectoId, fase, principal))
                .build());
        }

        return resumen;
    }

    public List<PasoResumenResponse> pasosDeFase(Long proyectoId, Fase fase, UserDetails principal) {
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        accesoFaseService.nivel(proyectoId, fase, principal); // valida el acceso a la fase

        Map<Long, Documento> documentos = documentosPorPaso(proyecto);
        List<Paso> pasos = pasoRepository.findByFaseOrderByOrdenAsc(fase);

        return pasos.stream().map(paso -> {
            Bloqueo bloqueo = bloqueoDe(paso, pasos, documentos);

            return PasoResumenResponse.builder()
                .pasoId(paso.getId())
                .orden(paso.getOrden())
                .tituloCorto(paso.getTituloCorto())
                .titulo(paso.getTitulo())
                .esProducto(paso.isEsProducto())
                .estado(estado(documentos.get(paso.getId())))
                .puedeCompletarse(bloqueo.permitido())
                .motivoBloqueo(bloqueo.motivo())
                .build();
        }).toList();
    }

    @Transactional
    public PasoDetalleResponse detalle(Long proyectoId, Long pasoId, UserDetails principal) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        NivelPermiso nivel = accesoFaseService.nivel(proyectoId, paso.getFase(), principal);

        Documento documento = obtenerOCrear(proyecto, paso);

        Map<Long, Documento> documentos = documentosPorPaso(proyecto);
        List<Paso> pasos = pasoRepository.findByFaseOrderByOrdenAsc(paso.getFase());
        Bloqueo bloqueo = bloqueoDe(paso, pasos, documentos);

        return PasoDetalleResponse.builder()
            .pasoId(paso.getId())
            .fase(paso.getFase())
            .faseNombre(paso.getFase().getNombre())
            .orden(paso.getOrden())
            .titulo(paso.getTitulo())
            .tituloCorto(paso.getTituloCorto())
            .esProducto(paso.isEsProducto())
            .explicacion(paso.getExplicacion())
            .ejemplo(paso.getEjemplo())
            .contenido(documento.getContenido())
            .version(documento.getVersion())
            .estado(estado(documento))
            .puedeCompletarse(bloqueo.permitido())
            .motivoBloqueo(bloqueo.motivo())
            .actualizadoEn(documento.getActualizadoEn())
            .actualizadoPor(documento.getActualizadoPor())
            .nivel(nivel)
            .puedeEditar(nivel == NivelPermiso.EDICION)
            .editandoOtro(otroEditando(proyectoId, pasoId, principal))
            .build();
    }

    /**
     * El historial es del dueño del proyecto: es la herramienta con la que mira quién escribió
     * qué, y no algo que cada colaborador tenga que poder consultar sobre los demás.
     */
    public List<VersionResponse> historial(Long proyectoId, Long pasoId, UserDetails principal) {
        findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.exigirDuenio(proyectoId, principal);

        Documento documento = documentoRepository
            .findByProyectoIdAndPasoId(proyecto.getId(), pasoId)
            .orElse(null);

        if (documento == null) return List.of();

        // Una de más: cada versión se compara contra la anterior, y la última de la página
        // necesita a su predecesora para saber qué agregó.
        List<DocumentoVersion> versiones = versionRepository.findByDocumentoOrderByIdDesc(
            documento, PageRequest.of(0, VERSIONES_EN_HISTORIAL + 1));

        int mostradas = Math.min(versiones.size(), VERSIONES_EN_HISTORIAL);
        List<VersionResponse> historial = new ArrayList<>(mostradas);

        for (int i = 0; i < mostradas; i++) {
            // La lista viene de la más nueva a la más vieja: la anterior está en i + 1.
            String previo = i + 1 < versiones.size() ? versiones.get(i + 1).getContenido() : null;
            historial.add(VersionResponse.from(versiones.get(i), previo));
        }

        return historial;
    }

    /** Un guardado en detalle: el texto que entró y el que salió, palabra por palabra. */
    public VersionDiffResponse diferencias(
        Long proyectoId, Long pasoId, Long versionId, UserDetails principal
    ) {
        findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.exigirDuenio(proyectoId, principal);

        Documento documento = documentoRepository
            .findByProyectoIdAndPasoId(proyecto.getId(), pasoId)
            .orElseThrow(() -> new ResourceNotFoundException("Este paso todavía no tiene guardados"));

        DocumentoVersion version = versionRepository.findById(versionId)
            .filter(candidata -> candidata.getDocumento().getId().equals(documento.getId()))
            .orElseThrow(() -> new ResourceNotFoundException(
                "Ese guardado no pertenece a este paso"));

        String previo = versionRepository
            .findFirstByDocumentoAndIdLessThanOrderByIdDesc(documento, versionId)
            .map(DocumentoVersion::getContenido)
            .orElse(null);

        return VersionDiffResponse.from(version, previo);
    }

    /**
     * El producto de la fase como archivo descargable. Sólo los productos: son el cierre de
     * cada fase, lo que se lleva y se comparte fuera de la aplicación.
     *
     * Alcanza con poder leer la fase: quien ve el texto en pantalla puede bajárselo.
     */
    public ExportacionService.Archivo exportar(
        Long proyectoId, Long pasoId, String formato, UserDetails principal
    ) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        accesoFaseService.nivel(proyectoId, paso.getFase(), principal);

        if (!paso.isEsProducto())
            throw new BusinessException("Sólo se pueden descargar los productos de cada fase");

        String contenido = documentoRepository
            .findByProyectoIdAndPasoId(proyecto.getId(), pasoId)
            .map(Documento::getContenido)
            .orElse("");

        return exportacionService.exportar(
            ExportacionService.Formato.de(formato),
            new ExportacionService.Datos(
                proyecto.getNombre(),
                paso.getFase().getNombre(),
                paso.getTituloCorto(),
                contenido
            )
        );
    }

    // --------------------------------------------------------------- escrituras

    @Transactional
    public PasoDetalleResponse guardar(
        Long proyectoId, Long pasoId, GuardarDocumentoRequest request, UserDetails principal
    ) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        accesoFaseService.exigirEdicion(proyectoId, paso.getFase(), principal);

        Documento documento = obtenerOCrear(proyecto, paso);

        // Bloqueo optimista: si alguien guardó mientras esta pestaña editaba, el cliente
        // se enteró tarde. Preferimos rechazar y avisar antes que pisar trabajo ajeno.
        if (!documento.getVersion().equals(request.getVersion()))
            throw new ConflictException(
                "Alguien más guardó cambios mientras editabas. Recargá para ver la última versión."
            );

        String autor = accesoFaseService.nombreDe(principal);
        // Nunca se guarda el HTML tal como llegó: ver HtmlSanitizer.
        String contenido = HtmlSanitizer.limpiar(request.getContenido());

        documento.setContenido(contenido);
        documento.setVersion(documento.getVersion() + 1);
        documento.setActualizadoEn(LocalDateTime.now());
        documento.setActualizadoPor(autor);
        documentoRepository.save(documento);

        versionRepository.save(DocumentoVersion.builder()
            .documento(documento)
            .contenido(contenido)
            .autor(autor)
            .autorTipo(accesoFaseService.tipoDe(principal))
            .creadoEn(LocalDateTime.now())
            .build());

        return detalle(proyectoId, pasoId, principal);
    }

    @Transactional
    public PasoDetalleResponse completar(
        Long proyectoId, Long pasoId, boolean completado, UserDetails principal
    ) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        accesoFaseService.exigirEdicion(proyectoId, paso.getFase(), principal);

        Documento documento = obtenerOCrear(proyecto, paso);

        if (completado) {
            Map<Long, Documento> documentos = documentosPorPaso(proyecto);
            List<Paso> pasos = pasoRepository.findByFaseOrderByOrdenAsc(paso.getFase());
            Bloqueo bloqueo = bloqueoDe(paso, pasos, documentos);

            if (!bloqueo.permitido())
                throw new BusinessException(bloqueo.motivo());
        }

        documento.setCompletado(completado);
        documentoRepository.save(documento);

        return detalle(proyectoId, pasoId, principal);
    }

    /** Latido de presencia: renueva la marca de "estoy editando esto". */
    @Transactional
    public String registrarPresencia(Long proyectoId, Long pasoId, UserDetails principal) {
        Paso paso = findPaso(pasoId);
        accesoFaseService.exigirEdicion(proyectoId, paso.getFase(), principal);

        redisService.hashSet(
            clavePresencia(proyectoId, pasoId),
            accesoFaseService.nombreDe(principal),
            String.valueOf(Instant.now().getEpochSecond()),
            PRESENCIA_TTL_MINUTOS
        );

        return otroEditando(proyectoId, pasoId, principal);
    }

    // ------------------------------------------------------------------ reglas

    /** Resultado de evaluar si un paso puede marcarse como completado. */
    private record Bloqueo(boolean permitido, String motivo) {
        static Bloqueo ok() { return new Bloqueo(true, null); }
        static Bloqueo no(String motivo) { return new Bloqueo(false, motivo); }
    }

    private Bloqueo bloqueoDe(Paso paso, List<Paso> pasosDeLaFase, Map<Long, Documento> documentos) {
        Documento propio = documentos.get(paso.getId());

        if (propio == null || !propio.tieneContenido())
            return Bloqueo.no("Escribí algo antes de darlo por terminado");

        // El producto es el cierre de la fase: exige todo el despliegue completo.
        if (paso.isEsProducto()) {
            boolean faltanPasos = pasosDeLaFase.stream()
                .filter(otro -> !otro.isEsProducto())
                .anyMatch(otro -> estado(documentos.get(otro.getId())) != EstadoPaso.COMPLETADO);

            return faltanPasos
                ? Bloqueo.no("Completá todos los pasos de la fase antes de cerrar el producto")
                : Bloqueo.ok();
        }

        // Los pasos no son secuenciales para editar, sólo para completar: alcanza con que el
        // anterior esté empezado.
        Paso anterior = pasosDeLaFase.stream()
            .filter(otro -> !otro.isEsProducto() && otro.getOrden() == paso.getOrden() - 1)
            .findFirst()
            .orElse(null);

        if (anterior == null) return Bloqueo.ok();

        Documento documentoAnterior = documentos.get(anterior.getId());
        boolean anteriorEmpezado = documentoAnterior != null && documentoAnterior.tieneContenido();

        return anteriorEmpezado
            ? Bloqueo.ok()
            : Bloqueo.no("Antes hay que empezar el paso " + anterior.getOrden()
                + ": " + anterior.getTituloCorto());
    }

    private EstadoPaso estado(Documento documento) {
        if (documento == null || !documento.tieneContenido()) return EstadoPaso.PENDIENTE;
        return documento.isCompletado() ? EstadoPaso.COMPLETADO : EstadoPaso.EN_PROGRESO;
    }

    // ------------------------------------------------------------------ apoyo

    private Paso findPaso(Long pasoId) {
        return pasoRepository.findById(pasoId)
            .orElseThrow(() -> new ResourceNotFoundException("Paso no encontrado con id: " + pasoId));
    }

    private Map<Long, Documento> documentosPorPaso(Proyecto proyecto) {
        return documentoRepository.findDelProyecto(proyecto).stream()
            .collect(Collectors.toMap(documento -> documento.getPaso().getId(), Function.identity()));
    }

    /**
     * El documento se materializa recién cuando alguien abre el paso. Con 37 pasos por
     * proyecto, crearlos todos de entrada sería llenar la tabla de filas vacías.
     */
    private Documento obtenerOCrear(Proyecto proyecto, Paso paso) {
        return documentoRepository.findByProyectoIdAndPasoId(proyecto.getId(), paso.getId())
            .orElseGet(() -> documentoRepository.save(Documento.builder()
                .proyecto(proyecto)
                .paso(paso)
                .build()));
    }

    /**
     * Quiénes más tienen el paso abierto. Es un hash con un campo por persona y el momento de
     * su último latido: con una sola clave por paso, el segundo en entrar pisaba al primero y
     * nadie veía a nadie.
     *
     * El TTL de la clave entera no alcanza para expirar a una persona que cerró la pestaña,
     * porque los latidos de las demás lo renuevan; por eso se descartan acá los latidos viejos.
     */
    private String otroEditando(Long proyectoId, Long pasoId, UserDetails principal) {
        String propio = accesoFaseService.nombreDe(principal);
        long corte = Instant.now().getEpochSecond() - PRESENCIA_VENCE_SEGUNDOS;

        String otros = redisService.hashGetAll(clavePresencia(proyectoId, pasoId)).entrySet().stream()
            .filter(entrada -> !entrada.getKey().toString().equals(propio))
            .filter(entrada -> latidoReciente(entrada.getValue().toString(), corte))
            .map(entrada -> entrada.getKey().toString())
            .sorted()
            .collect(Collectors.joining(", "));

        return otros.isBlank() ? null : otros;
    }

    private boolean latidoReciente(String epoch, long corte) {
        try {
            return Long.parseLong(epoch) >= corte;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String clavePresencia(Long proyectoId, Long pasoId) {
        return PRESENCIA + ":" + proyectoId + ":" + pasoId;
    }
}
