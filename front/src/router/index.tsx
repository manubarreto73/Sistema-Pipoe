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
                    lazy: async () => ({
                      Component: (await import("@/pages/MisProyectos")).default,
                    }),
                  },
                  {
                    // Nombre, colaboradores y borrado. Es cosa del dueño: la API le da 403
                    // al colaborador en cada una de las tres.
                    path: "proyectos/:proyectoId/configuracion",
                    lazy: async () => ({
                      Component: (await import("@/pages/ProyectoConfiguracion")).default,
                    }),
                  },
                ],
              },
              {
                // Marco de trabajo del proyecto: encabezado y barra de fases alrededor de
                // la portada, el flujo de una fase y el documento de un paso.
                path: "proyectos/:proyectoId",
                lazy: async () => ({
                  Component: (await import("@/router/ProyectoLayout")).ProyectoLayout,
                }),
                children: [
                  {
                    index: true,
                    lazy: async () => ({
                      Component: (await import("@/pages/ProyectoInicio")).default,
                    }),
                  },
                  {
                    path: "fases/:fase",
                    lazy: async () => ({
                      Component: (await import("@/pages/FaseFlujo")).default,
                    }),
                  },
                  {
                    path: "pasos/:pasoId",
                    lazy: async () => ({
                      Component: (await import("@/pages/Paso")).default,
                    }),
                  },
                ],
              },
              {
                path: "perfil",
                lazy: async () => ({
                  Component: (await import("@/pages/Perfil")).default,
                }),
              },
              {
                // Sirve a los dos tipos de sesión: la API resuelve por el principal.
                path: "cambiar-password",
                lazy: async () => ({
                  Component: (await import("@/pages/CambiarPassword")).default,
                }),
              },
              {
                // Espeja el @PreAuthorize("hasRole('ADMIN')") del controller. La API
                // sigue siendo la autoridad: esto sólo evita mostrar una pantalla
                // que devolvería 403.
                element: <ProtectedRoute allow={["USUARIO"]} roles={["ADMIN"]} />,
                children: [
                  {
                    path: "admin/solicitudes",
                    lazy: async () => ({
                      Component: (await import("@/pages/AdminSolicitudes")).default,
                    }),
                  },
                  {
                    // El contenido del modelo PipoE: explicación y ejemplo de cada paso.
                    path: "admin/catalogo",
                    lazy: async () => ({
                      Component: (await import("@/pages/AdminCatalogo")).default,
                    }),
                  },
                  {
                    // Los textos de la portada pública.
                    path: "admin/landing",
                    lazy: async () => ({
                      Component: (await import("@/pages/AdminLanding")).default,
                    }),
                  },
                  {
                    // Los cupos de la aplicación.
                    path: "admin/ajustes",
                    lazy: async () => ({
                      Component: (await import("@/pages/AdminAjustes")).default,
                    }),
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
