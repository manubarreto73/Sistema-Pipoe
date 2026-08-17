import { Link, Outlet } from "react-router";

import { Button } from "@/components/ui/Button";
import { Marca } from "@/components/ui/Marca";

export function PublicLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-white">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
        <nav className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <Link
            to="/"
            className="rounded focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-600"
          >
            <Marca />
          </Link>

          <div className="flex items-center gap-2">
            <Link to="/login">
              <Button variant="ghost">Ya tengo cuenta</Button>
            </Link>
            <Link to="/pedir-acceso">
              <Button>Pedir acceso</Button>
            </Link>
          </div>
        </nav>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-slate-50">
        <div className="mx-auto flex max-w-5xl flex-col gap-4 px-6 py-8 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-slate-500">
            Modelo PipoE — Arlette Pichardo Muñiz
          </p>

          <a
            href="https://arlettepichardo.com/"
            target="_blank"
            rel="noreferrer"
            className="text-sm font-medium text-brand-700 hover:underline"
          >
            arlettepichardo.com ↗
          </a>
        </div>
      </footer>
    </div>
  );
}
