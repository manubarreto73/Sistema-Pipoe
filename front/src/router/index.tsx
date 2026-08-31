import { createBrowserRouter } from "react-router";

import Landing from "@/pages/Landing";
import LoginColaborador from "@/pages/LoginColaborador";
import LoginUsuario from "@/pages/LoginUsuario";
import NotFound from "@/pages/NotFound";
import PedirAcceso from "@/pages/PedirAcceso";
import { AppLayout } from "@/router/AppLayout";
import { ProtectedRoute } from "@/router/ProtectedRoute";
import { Providers } from "@/router/Providers";
import { PublicLayout } from "@/router/PublicLayout";
import { pagina } from "@/lib/pagina";

export const router = createBrowserRouter([
  {
    element: <Providers />,
    children: [
      // Público. Import directo: son las primeras pantallas que ve cualquiera,
      // partirlas en chunks solo agregaría un round-trip antes del primer render.
      {
        element: <PublicLayout />,
        children: [
          { index: true, element: <Landing /> },
          { path: "login", element: <LoginUsuario /> },
          { path: "login/colaborador", element: <LoginColaborador /> },
          { path: "pedir-acceso", element: <PedirAcceso /> },
        ],
      },

      // Privado. Acá sí conviene `lazy`: son las pantallas que van a crecer y no
      // tiene sentido descargarlas antes de que el usuario esté autenticado.
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <AppLayout />,
            children: [
              {
                // "Mis proyectos" no aplica a un colaborador: su sesión está atada
                // a un único proyecto, así que ProtectedRoute lo redirige al suyo.
                // Tampoco a un ADMIN: su cuenta sólo gestiona solicitudes.
                element: <ProtectedRoute allow={["USUARIO"]} roles={["USER"]} />,
                children: [
                  {
                    path: "proyectos",
                    lazy: pagina(() => import("@/pages/MisProyectos")),
                  },
                  {
                    // Nombre, colaboradores y borrado. Es cosa del dueño: la API le da 403
                    // al colaborador en cada una de las tres.
                    path: "proyectos/:proyectoId/configuracion",
                    lazy: pagina(() => import("@/pages/ProyectoConfiguracion")),
                  },
                ],
              },
              {
                // Marco de trabajo del proyecto: encabezado y barra de fases alrededor de
                // la portada, el flujo de una fase y el documento de un paso.
                path: "proyectos/:proyectoId",
                lazy: pagina(async () => ({
                  default: (await import("@/router/ProyectoLayout")).ProyectoLayout,
                })),
                children: [
                  {
                    index: true,
                    lazy: pagina(() => import("@/pages/ProyectoInicio")),
                  },
                  {
                    path: "fases/:fase",
                    lazy: pagina(() => import("@/pages/FaseFlujo")),
                  },
                  {
                    path: "pasos/:pasoId",
                    lazy: pagina(() => import("@/pages/Paso")),
                  },
                ],
              },
              {
                path: "perfil",
                lazy: pagina(() => import("@/pages/Perfil")),
              },
              {
                // Sirve a los dos tipos de sesión: la API resuelve por el principal.
                path: "cambiar-password",
                lazy: pagina(() => import("@/pages/CambiarPassword")),
              },
              {
                // Espeja el @PreAuthorize("hasRole('ADMIN')") del controller. La API
                // sigue siendo la autoridad: esto sólo evita mostrar una pantalla
                // que devolvería 403.
                element: <ProtectedRoute allow={["USUARIO"]} roles={["ADMIN"]} />,
                children: [
                  {
                    // Todos los proyectos del sistema. Entrar en uno la lleva a la misma
                    // vista de trabajo que ve el equipo, pero con permiso de comentario.
                    path: "admin/proyectos",
                    lazy: pagina(() => import("@/pages/AdminProyectos")),
                  },
                  {
                    // El padrón de cuentas: alta de administradores, baja y alta lógica.
                    path: "admin/usuarios",
                    lazy: pagina(() => import("@/pages/AdminUsuarios")),
                  },
                  {
                    path: "admin/solicitudes",
                    lazy: pagina(() => import("@/pages/AdminSolicitudes")),
                  },
                  {
                    // El contenido del modelo PipoE: explicación y ejemplo de cada paso.
                    path: "admin/catalogo",
                    lazy: pagina(() => import("@/pages/AdminCatalogo")),
                  },
                  {
                    // Los textos de la portada pública.
                    path: "admin/landing",
                    lazy: pagina(() => import("@/pages/AdminLanding")),
                  },
                  {
                    // Los cupos de la aplicación.
                    path: "admin/ajustes",
                    lazy: pagina(() => import("@/pages/AdminAjustes")),
                  },
                ],
              },
            ],
          },
        ],
      },

      { path: "*", element: <NotFound /> },
    ],
  },
]);
