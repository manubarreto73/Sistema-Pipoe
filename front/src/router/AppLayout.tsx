import { useState } from "react";
import { Link, Outlet } from "react-router";

import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Marca } from "@/components/ui/Marca";
import { destinoInicial, useLogout } from "@/features/auth/hooks";
import { useAuthStore } from "@/stores/auth";

export function AppLayout() {
  const sesion = useAuthStore((state) => state.sesion);
  const logout = useLogout();
  const [confirmandoSalida, setConfirmandoSalida] = useState(false);

  const inicio = sesion ? destinoInicial(sesion) : "/proyectos";

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <Link
            to={inicio}
            className="focus-visible:outline-brand-600 rounded focus-visible:outline-2 focus-visible:outline-offset-4"
          >
            <Marca />
          </Link>

          <div className="flex items-center gap-2">
            {sesion?.role === "ADMIN" && (
              <>
                <Link to="/admin/proyectos">
                  <Button variant="ghost">Proyectos</Button>
                </Link>
                <Link to="/admin/solicitudes">
                  <Button variant="ghost">Solicitudes</Button>
                </Link>
                <Link to="/admin/usuarios">
                  <Button variant="ghost">Usuarios</Button>
                </Link>
                <Link to="/admin/catalogo">
                  <Button variant="ghost">Catálogo</Button>
                </Link>
                <Link to="/admin/landing">
                  <Button variant="ghost">Portada</Button>
                </Link>
                <Link to="/admin/ajustes">
                  <Button variant="ghost">Ajustes</Button>
                </Link>
              </>
            )}

            {sesion?.role === "USER" && (
              <Link to="/proyectos">
                <Button variant="ghost">Mis proyectos</Button>
              </Link>
            )}

            {/* La identidad de la sesión y el acceso al perfil son la misma cosa: mostrar el
                nombre en un lado y un botón "Mi perfil" en otro era repetir lo mismo dos veces. */}
            <Link
              to="/perfil"
              title="Mi perfil"
              className="focus-visible:outline-brand-600 flex items-center gap-2 rounded-lg py-1.5 pr-3 pl-1.5 transition-colors hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              <IconoPerfil />

              <span className="hidden text-left sm:block">
                <span className="block text-sm font-medium text-slate-800">
                  {sesion?.nombreCompleto}
                </span>
                {sesion?.type === "COLABORADOR" && sesion.proyectoNombre && (
                  <span className="block text-xs text-slate-500">{sesion.proyectoNombre}</span>
                )}
              </span>
            </Link>

            <Button
              variant="secondary"
              loading={logout.isPending}
              onClick={() => setConfirmandoSalida(true)}
            >
              Cerrar sesión
            </Button>
          </div>
        </nav>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-6 py-10">
        <Outlet />
      </main>

      {/* El logout no falla nunca de cara al usuario: useLogout limpia igual si la API no
          responde, así que el diálogo no necesita mostrar error. */}
      <ConfirmDialog
        open={confirmandoSalida}
        title="Cerrar sesión"
        description="Vas a volver a la pantalla de inicio de sesión."
        confirmLabel="Cerrar sesión"
        cancelLabel="Seguir acá"
        loading={logout.isPending}
        onCancel={() => setConfirmandoSalida(false)}
        onConfirm={() => logout.mutate()}
      />
    </div>
  );
}

/** Silueta en un círculo. Marca el acceso al perfil aun cuando el nombre no entra en pantalla. */
function IconoPerfil() {
  return (
    <span
      aria-hidden
      className="bg-brand-100 text-brand-700 flex size-8 shrink-0 items-center justify-center rounded-full"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.8}
        strokeLinecap="round"
        strokeLinejoin="round"
        className="size-4.5"
      >
        <circle cx="12" cy="8" r="3.5" />
        <path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
      </svg>
    </span>
  );
}
