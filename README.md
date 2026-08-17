# Sistema Pipoe

Aplicación web para llevar adelante procesos de planificación con el **Modelo PipoE**, creado por
la Dra. Arlette Pichardo Muñiz.

El modelo se sustenta en la articulación de cinco componentes esenciales —no etapas—: Promoción,
indagación, programación, organización —incluidas la coordinación y la administración— y
Evaluación. De las iniciales de esos componentes deriva la sigla PipoE. Sus usos abarcan desde la
planificación de una acción de la vida cotidiana o una actividad institucional u organizativa
hasta una política pública, pasando por proyectos, programas y planes.

El sistema traslada ese recorrido a un espacio de trabajo compartido: acompaña a quien planifica
paso por paso, guarda lo que va escribiendo, permite trabajarlo en equipo y entrega los productos
de cada componente listos para presentar.

---

## Funcionalidades

### Planificar siguiendo el modelo

- **Un proyecto por cada proceso de planificación.** Cada persona puede llevar varios proyectos en
  paralelo, cada uno con su propio equipo y su propio avance.
- **Los cinco componentes, con sus pasos en orden.** El proyecto se recorre componente por
  componente y, dentro de cada uno, paso por paso. En todo momento se ve cuánto se avanzó en cada
  componente.
- **Cada paso, una hoja de trabajo.** Un editor de texto con formato —títulos, negritas, listas,
  citas— donde se escribe la respuesta a ese paso. Lo escrito se guarda y se retoma cuando se
  quiera.
- **Explicación y ejemplo a mano.** Cada paso puede acompañarse de una explicación de qué se
  espera y de un ejemplo, disponibles junto a la hoja de trabajo sin salir de ella.
- **Marcar un paso como completado**, para distinguir lo cerrado de lo que sigue en borrador.

### Obtener los productos

- **Cada componente termina en un producto**: el plan de promoción, el diagnóstico situacional, el
  plan de acción, el plan de organización y el diseño de evaluación.
- **Descarga en Word o PDF**, para presentar, imprimir o seguir trabajando fuera del sistema.

### Trabajar en equipo

- **Invitar colaboradores al proyecto.** Cada uno recibe sus credenciales por correo y entra a ese
  proyecto, no al resto.
- **Permisos componente por componente.** A cada colaborador se le define, para cada uno de los
  cinco componentes, si puede sólo leer o también editar. Así una persona puede aportar en la
  indagación sin tocar la evaluación.
- **Aviso de edición simultánea.** Si dos personas abren el mismo paso al mismo tiempo, cada una ve
  quién más lo está editando; y si alguien guarda mientras otro escribía, el sistema lo advierte en
  lugar de pisar el trabajo ajeno.
- **Historial de cambios.** Quien creó el proyecto puede ver cada guardado, quién lo hizo y cuándo,
  y exactamente qué texto se agregó y cuál se quitó respecto de la versión anterior.

### Entrar al sistema

- **El acceso es por solicitud.** Cualquiera puede pedirlo desde la portada completando un
  formulario con sus datos, su ocupación y los usos que piensa darle al modelo.
- **La solicitud la resuelve la administración.** Al aprobarla, la persona recibe sus credenciales
  por correo.

### Administrar

- **Revisar las solicitudes de acceso**, con búsqueda por nombre, correo o institución, filtros por
  fecha y por estado, y la posibilidad de aprobar o rechazar cada una.
- **Editar los textos de la portada** —presentación del modelo, biografía— desde el propio sistema,
  sin tocar el código.
- **Editar el catálogo de pasos**: el enunciado, la explicación y el ejemplo de cada uno de los
  pasos del modelo, que se reflejan en todos los proyectos.
- **Definir los cupos**: cuántos proyectos puede tener una persona y cuántos colaboradores puede
  sumar a cada proyecto.

---

## Stack técnico

| Capa | Tecnologías |
|---|---|
| **Backend** | Java 17 · Spring Boot 4 · Spring Security · Spring Data JPA / Hibernate · Maven |
| **Base de datos** | PostgreSQL · Flyway para el versionado del esquema |
| **Sesiones y control de flujo** | Redis |
| **Autenticación** | JSON Web Tokens con refresh token rotativo |
| **Documentos** | jsoup · Apache POI (Word) · openhtmltopdf (PDF) |
| **Correo** | Spring Mail sobre SMTP |
| **Frontend** | React 19 · TypeScript · Vite · React Router |
| **Estado y datos en el front** | TanStack Query · Zustand |
| **Formularios y validación** | React Hook Form · Zod |
| **Estilos** | Tailwind CSS |
| **Editor de texto** | TipTap |

---

## Arquitectura

El sistema son dos piezas independientes: una **API REST sin estado** y una **aplicación de página
única** que la consume. No comparten código ni sesión de servidor; el único vínculo es el contrato
HTTP.

### La API

Sigue una **arquitectura en capas**, y se organiza **por dominio** antes que por capa: cada
agregado del negocio es un paquete propio que contiene adentro todas sus capas.

| Capa | Responsabilidad |
|---|---|
| **Controller** | Recibe la petición HTTP, valida el formato de entrada y decide el código de respuesta. No contiene reglas de negocio. |
| **Service** | Las reglas del negocio y los límites de la transacción. Es la única capa que decide. |
| **Repository** | El acceso a la base de datos, sobre Spring Data JPA. |
| **Entity** | El modelo persistente, mapeado contra las tablas. |
| **DTO** | Lo que entra y lo que sale por HTTP, separado del modelo persistente para que la forma de la base no quede expuesta en la API. |

Los dominios son: autenticación, usuarios, proyectos, colaboradores, pasos, documentos, solicitudes
de acceso, textos de la portada y parámetros del sistema.

Alrededor de esas capas hay tres piezas transversales: el **filtro de autenticación**, que resuelve
la identidad de cada petición antes de que llegue al controlador; un **manejador global de
excepciones**, que traduce cualquier error a una respuesta uniforme; y el **acceso a Redis**, donde
viven los refresh tokens, el control de intentos de acceso y la presencia de quien está editando.

La autorización tiene dos niveles. El **rol** define a qué endpoints se puede llegar —administración
o persona usuaria—; y dentro de un proyecto, un **control de acceso por componente** decide, para
cada petición, si quien la hace puede leer o escribir ese componente en ese proyecto en particular.

### El frontend

Aplicación de página única, organizada **por funcionalidad**: cada área del sistema agrupa sus
llamadas a la API, sus hooks de datos y sus esquemas de validación, y aparte hay un catálogo de
componentes de interfaz reutilizables. El estado del servidor se maneja con caché y revalidación —no
se duplica en el estado local—, y lo único que se guarda en el navegador es la sesión.

---

## Modelo de datos

Base de datos **relacional en PostgreSQL**. El esquema se versiona con migraciones de Flyway.

### `usuarios`

Las personas con cuenta propia en el sistema: la administración y quienes crean proyectos.

| Campo | Tipo | Descripción |
|---|---|---|
| `usuario_id` | BIGINT | Identificador, generado por la base. |
| `email` | VARCHAR(150) | Correo, único. Es el identificador de acceso. |
| `nombre_completo` | VARCHAR(150) | Nombre y apellidos. |
| `password` | VARCHAR(255) | Contraseña cifrada con BCrypt. |
| `role` | VARCHAR(20) | `ADMIN` o `USER`. |
| `enabled` | BOOLEAN | Si la cuenta puede iniciar sesión. |

### `proyectos`

Cada proceso de planificación.

| Campo | Tipo | Descripción |
|---|---|---|
| `proyecto_id` | BIGINT | Identificador. |
| `nombre` | VARCHAR(100) | Nombre del proyecto, único en todo el sistema. |
| `usuario_id` | BIGINT | Quien lo creó. Es su dueño. |

### `colaboradores`

Las personas invitadas a un proyecto. Tienen credenciales propias y acceso a ese proyecto
únicamente; no son usuarios del sistema.

| Campo | Tipo | Descripción |
|---|---|---|
| `colaborador_id` | BIGINT | Identificador. |
| `nombre` | VARCHAR(100) | Nombre de la persona. |
| `email` | VARCHAR(150) | Correo. Único dentro del proyecto. |
| `password` | VARCHAR(255) | Contraseña cifrada, generada al invitarla. |
| `activo` | BOOLEAN | Si sigue teniendo acceso. |
| `proyecto_id` | BIGINT | Proyecto al que pertenece. |

### `colaborador_permisos`

Qué puede hacer un colaborador en cada componente. Una fila por colaborador y componente.

| Campo | Tipo | Descripción |
|---|---|---|
| `permiso_id` | BIGINT | Identificador. |
| `colaborador_id` | BIGINT | Colaborador al que aplica. |
| `fase` | VARCHAR(20) | Componente: `PROMOCION`, `INDAGACION`, `PROGRAMACION`, `ORGANIZACION` o `EVALUACION`. |
| `nivel` | VARCHAR(20) | `LECTURA` o `EDICION`. |

### `pasos`

El catálogo del modelo: los pasos que componen cada componente. Es común a todos los proyectos.

| Campo | Tipo | Descripción |
|---|---|---|
| `paso_id` | BIGINT | Identificador. |
| `fase` | VARCHAR(20) | Componente al que pertenece. |
| `orden` | INTEGER | Posición dentro del componente. |
| `titulo` | TEXT | El enunciado completo del paso. |
| `titulo_corto` | VARCHAR(60) | Versión breve, para la navegación. |
| `explicacion` | TEXT | Qué se espera en ese paso. |
| `ejemplo` | TEXT | Un ejemplo de referencia. |
| `es_producto` | BOOLEAN | Si es el producto final del componente. |

### `documentos`

Lo que un proyecto escribió en un paso. Una fila por proyecto y paso.

| Campo | Tipo | Descripción |
|---|---|---|
| `documento_id` | BIGINT | Identificador. |
| `proyecto_id` | BIGINT | Proyecto al que pertenece. |
| `paso_id` | BIGINT | Paso que responde. |
| `contenido` | TEXT | El texto con formato. |
| `completado` | BOOLEAN | Si se marcó como terminado. |
| `version` | INTEGER | Número de guardado, para detectar ediciones simultáneas. |
| `actualizado_en` | TIMESTAMP | Fecha del último guardado. |
| `actualizado_por` | VARCHAR(150) | Quién guardó por última vez. |

### `documento_versiones`

El historial: una fila por cada guardado.

| Campo | Tipo | Descripción |
|---|---|---|
| `version_id` | BIGINT | Identificador. |
| `documento_id` | BIGINT | Documento al que pertenece. |
| `contenido` | TEXT | El texto tal como quedó en ese guardado. |
| `autor` | VARCHAR(150) | Quién lo guardó. |
| `autor_tipo` | VARCHAR(20) | Si fue el dueño del proyecto o un colaborador. |
| `creado_en` | TIMESTAMP | Cuándo. |

### `solicitudes_acceso`

Los pedidos de acceso enviados desde la portada.

| Campo | Tipo | Descripción |
|---|---|---|
| `solicitud_acceso_id` | BIGINT | Identificador. |
| `nombre` | VARCHAR(150) | Nombre. |
| `apellidos` | VARCHAR(150) | Apellidos. |
| `email` | VARCHAR(150) | Correo de contacto. |
| `genero` | VARCHAR(20) | Género. |
| `rango_edad` | VARCHAR(20) | Rango de edad. |
| `nivel_instruccion` | VARCHAR(30) | Nivel de instrucción alcanzado. |
| `ocupacion` | VARCHAR(40) | Ocupación. |
| `ocupacion_otra` | VARCHAR(150) | Ocupación escrita a mano, si eligió "otra". |
| `institucion` | VARCHAR(200) | Institución u organización. |
| `pais_nacimiento` | VARCHAR(100) | País de nacimiento. |
| `pais_residencia` | VARCHAR(100) | País de residencia. |
| `motivacion` | VARCHAR(1000) | Por qué quiere usar el modelo. |
| `usos_otro` | VARCHAR(150) | Uso escrito a mano, si eligió "otro". |
| `canal_otro` | VARCHAR(150) | Canal escrito a mano, si eligió "otro". |
| `estado` | VARCHAR(20) | `PENDIENTE`, `APROBADA` o `RECHAZADA`. |
| `fecha_solicitud` | TIMESTAMP | Cuándo se envió. |
| `fecha_resolucion` | TIMESTAMP | Cuándo se resolvió. |

### `solicitud_usos` y `solicitud_canales`

Las respuestas de opción múltiple de una solicitud: los usos que le dará al modelo, y por qué vías
se enteró de él. Una fila por opción marcada.

| Campo | Tipo | Descripción |
|---|---|---|
| `solicitud_acceso_id` | BIGINT | Solicitud a la que pertenece. |
| `uso` / `canal` | VARCHAR(40) | La opción marcada. |

### `textos_landing`

Los textos de la portada, editables desde la administración.

| Campo | Tipo | Descripción |
|---|---|---|
| `clave` | VARCHAR(50) | Qué texto es: el título, la descripción, la biografía. |
| `contenido` | TEXT | El texto. |
| `actualizado_en` | TIMESTAMP | Última edición. |

### `parametros`

Los cupos del sistema. Una única fila.

| Campo | Tipo | Descripción |
|---|---|---|
| `parametro_id` | BIGINT | Identificador. |
| `max_proyectos_por_usuario` | INTEGER | Cuántos proyectos puede crear una persona. |
| `max_colaboradores_por_proyecto` | INTEGER | Cuántos colaboradores admite un proyecto. |

---

## Endpoints

Todos bajo `/api`. Salvo los marcados como públicos, requieren un token de acceso.

### Autenticación · `/api/auth`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/login` | Inicio de sesión de un usuario. Público. |
| `POST` | `/colaborador/login` | Inicio de sesión de un colaborador. Público. |
| `POST` | `/refresh` | Renueva el token de acceso y rota el de refresco. Público. |
| `POST` | `/logout` | Cierra la sesión y anula el token de refresco. |
| `GET` | `/me` | Los datos de la sesión en curso. |
| `PUT` | `/perfil` | Cambia el nombre de la cuenta. |
| `PUT` | `/change-password` | Cambia la contraseña, pidiendo la actual. |
| `POST` | `/register` | Alta de un usuario. Sólo administración. |

### Solicitudes de acceso · `/api/solicitudes-acceso`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/` | Envía una solicitud desde la portada. Público. |
| `GET` | `/` | Listado con búsqueda, filtros por estado y por fecha. Sólo administración. |
| `POST` | `/{id}/aprobar` | Aprueba la solicitud, crea el usuario y le envía sus credenciales. Sólo administración. |
| `POST` | `/{id}/rechazar` | Rechaza la solicitud. Sólo administración. |

### Proyectos · `/api/proyectos`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Los proyectos de quien consulta. |
| `POST` | `/` | Crea un proyecto. |
| `GET` | `/{id}` | Un proyecto. Accesible también por sus colaboradores. |
| `PUT` | `/{id}` | Renombra el proyecto. |
| `DELETE` | `/{id}` | Borra el proyecto y todo su contenido. |

### Colaboradores · `/api/proyectos/{proyectoId}/colaboradores`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Los colaboradores del proyecto y sus permisos. |
| `POST` | `/` | Invita a un colaborador y le envía sus credenciales. |
| `PUT` | `/{colaboradorId}/permisos` | Cambia sus permisos por componente. |
| `DELETE` | `/{colaboradorId}` | Le quita el acceso al proyecto. |

### Trabajo en el proyecto · `/api/proyectos/{proyectoId}`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/fases` | Los cinco componentes con el avance de cada uno. |
| `GET` | `/fases/{fase}/pasos` | Los pasos de un componente, en orden, con su estado. |
| `GET` | `/pasos/{pasoId}` | Un paso: su enunciado, su explicación, su ejemplo y lo escrito. |
| `PUT` | `/pasos/{pasoId}/documento` | Guarda lo escrito. Avisa si alguien guardó mientras tanto. |
| `PUT` | `/pasos/{pasoId}/completado` | Marca o desmarca el paso como completado. |
| `GET` | `/pasos/{pasoId}/versiones` | El historial de guardados. Sólo el dueño del proyecto. |
| `GET` | `/pasos/{pasoId}/versiones/{versionId}` | Qué se agregó y qué se quitó en ese guardado. |
| `GET` | `/pasos/{pasoId}/exportar` | Descarga el producto del componente en Word o PDF. |
| `POST` | `/pasos/{pasoId}/presencia` | Informa que se está editando y devuelve quién más lo está haciendo. |

### Catálogo de pasos · `/api/catalogo/pasos`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Todos los pasos del modelo. Sólo administración. |
| `PUT` | `/{pasoId}` | Edita el enunciado, la explicación y el ejemplo de un paso. Sólo administración. |

### Textos de la portada · `/api/landing/textos`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Los textos de la portada. Público. |
| `PUT` | `/{clave}` | Edita uno de los textos. Sólo administración. |

### Parámetros · `/api/parametros`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Los cupos vigentes. |
| `PUT` | `/` | Cambia los cupos. Sólo administración. |
