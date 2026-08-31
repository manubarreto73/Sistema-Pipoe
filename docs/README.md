# Documentación del Proyecto Pipoe

| Documento | Qué es |
|---|---|
| [textos-arlette.md](textos-arlette.md) | Los textos de Arlette, transcriptos literales. Ver la regla abajo |
| [plan-visual.md](plan-visual.md) | Plan para alinear la identidad gráfica con la de Arlette |
| [plan-seguridad.md](plan-seguridad.md) | Auditoría de vulnerabilidades y plan de corrección |
| [plan-deploy.md](plan-deploy.md) | Qué contratar y en qué orden para salir a producción |
| [correo.md](correo.md) | Cómo se envían las contraseñas: Brevo, DNS y prueba local |
| [../DEPLOY.md](../DEPLOY.md) | El *cómo* del deploy: Dockerfiles, compose, nginx, backups, CI |

Los tres planes son documentos de trabajo: se van tachando a medida que se ejecutan.

---

## Los textos de Arlette

Esta carpeta guarda **los textos escritos por Arlette Pichardo Muñiz**, tal cual los entregó.

## Por qué existe

El sitio habla del Modelo PipoE, y las definiciones del modelo son de ella. Cualquier
descripción que no salga de estos archivos es una interpretación, y una interpretación puede
errarle a la forma en que ella define las cosas. Todo lo que se publica tiene que poder
aprobarlo Arlette.

## La regla

**Nada de lo que se muestra al público se redacta acá.** Si hace falta un texto que no está en
esta carpeta, se le pide a Arlette y recién después se publica.

Eso incluye descripciones del modelo, de sus componentes, de sus fases o de sus pasos:
títulos, ideas centrales, explicaciones y ejemplos. Si un lugar de la interfaz necesita un
texto que todavía no existe, se deja vacío o con un cartel de "todavía no está cargado" —nunca
con un relleno inventado.

Lo que sí se escribe sin consultar es la **copia funcional de la interfaz**: rótulos de botones
("Guardar", "Agregar colaborador"), mensajes de error, ayudas de un formulario y confirmaciones.
Son instrucciones de uso de la aplicación, no contenido del modelo.

## Archivos

| Archivo | Qué contiene | Dónde se usa |
| --- | --- | --- |
| [textos-arlette.md](textos-arlette.md) | Descripción del modelo, biografía y formulario de acceso | La portada (ver abajo) y `front/src/pages/PedirAcceso.tsx` + `front/src/features/solicitudes/types.ts` |

## Los textos de la portada se editan desde la aplicación

Los tres textos de la portada —descripción, modelo y biografía— ya no están escritos en el
código. Viven en la tabla `textos_landing` y se editan desde la cuenta de administradora, en
**Portada** (`/admin/landing`). La semilla original está en la migración
`api/src/main/resources/db/migration/V10__textos_landing.sql`, copiada de este archivo.

Este archivo sigue siendo la transcripción de referencia: si alguien edita la portada por error,
acá está el texto original para restituirlo.

## Contenido del modelo que no está acá

Los **títulos de los 32 pasos y los 5 productos** de las fases salieron de la planilla que
mandó Arlette y viven en la migración `api/src/main/resources/db/migration/V8__pasos_y_documentos.sql`.
Las **ideas centrales** de las 5 fases ("Compromiso para la acción", "Estado de situación", …)
salieron de la misma planilla y viven en `api/.../dominio/pasos/entities/Fase.java`.

Las **explicaciones y los ejemplos** de cada paso los carga Arlette desde la pantalla
`/admin/catalogo`. La aplicación no trae ninguno escrito de fábrica: los pasos sin cargar
muestran "Este paso todavía no tiene cargada su explicación ni su ejemplo".
