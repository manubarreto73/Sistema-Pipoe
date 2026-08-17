import { Navigate, Outlet, useLocation } from "react-router";

import { destinoInicial } from "@/features/auth/hooks";
import type { Role, SessionType } from "@/features/auth/types";
import { useAuthStore } from "@/stores/auth";

type ProtectedRouteProps = {
  /** Tipos de sesión admitidos. Sin este prop, alcanza con estar autenticado. */
  allow?: SessionType[];
  /** Roles admitidos. Los colaboradores tienen `role: null`, así que nunca pasan. */
  roles?: Role[];
};

export function ProtectedRoute({ allow, roles }: ProtectedRouteProps) {
  const sesion = useAuthStore((state) => state.sesion);
  const location = useLocation();

  if (!sesion) {
    // `replace` evita que el botón "atrás" devuelva a la ruta protegida en loop.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  const destinoPorDefecto = destinoInicial(sesion);

  if (allow && !allow.includes(sesion.type)) {
    return <Navigate to={destinoPorDefecto} replace />;
  }

  if (roles && (sesion.role === null || !roles.includes(sesion.role))) {
    return <Navigate to={destinoPorDefecto} replace />;
  }

  return <Outlet />;
}
