package com.pipoe.pipoeapi.dominio.proyectos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    boolean existsByNombre(String nombre);
    /** Para renombrar: el nombre propio no cuenta como colisión. */
    boolean existsByNombreAndIdNot(String nombre, Long id);
    Optional<Proyecto> findByNombre(String nombre);
    /** Login del colaborador. El código llega ya normalizado por CodigoProyectoGenerator. */
    Optional<Proyecto> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    long countByUsuario(Usuario usuario);
    List<Proyecto> findByUsuarioOrderByNombreAsc(Usuario usuario);

    /**
     * Listado de la administradora: todos los proyectos, con búsqueda y filtro de terminado.
     *
     * `texto` busca en el nombre del proyecto, en su código y en el nombre del dueño, y llega ya
     * en minúsculas y con los comodines puestos, igual que en SolicitudAccesoRepository.
     *
     * "Terminado" es tener completados los productos de las cinco fases. Se resuelve con una
     * subconsulta y no filtrando en memoria porque es un filtro: aplicarlo después de paginar
     * devolvería páginas de tamaño variable y un total mentiroso.
     *
     * `fases` es cuántas hay (Fase.values().length) y entra por parámetro para que el día que el
     * modelo cambie no haya un 5 escondido en un JPQL.
     *
     * El JOIN FETCH del dueño convive con Pageable porque es un ManyToOne: no multiplica filas.
     */
    @Query("""
        SELECT p FROM Proyecto p
        JOIN FETCH p.usuario u
        WHERE (CAST(:texto AS string) IS NULL
               OR LOWER(p.nombre) LIKE :texto
               OR LOWER(p.codigo) LIKE :texto
               OR LOWER(u.nombreCompleto) LIKE :texto)
          AND (:terminado IS NULL
               OR (:terminado = TRUE AND :fases = (
                     SELECT COUNT(d) FROM Documento d JOIN d.paso ps
                     WHERE d.proyecto = p AND ps.esProducto = TRUE AND d.completado = TRUE))
               OR (:terminado = FALSE AND :fases > (
                     SELECT COUNT(d) FROM Documento d JOIN d.paso ps
                     WHERE d.proyecto = p AND ps.esProducto = TRUE AND d.completado = TRUE)))
        """)
    Page<Proyecto> buscarParaAdmin(
        @Param("texto") String texto,
        @Param("terminado") Boolean terminado,
        @Param("fases") long fases,
        Pageable pageable
    );
}
