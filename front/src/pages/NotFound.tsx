import { Link } from "react-router";

import { Button } from "@/components/ui/Button";

export default function NotFound() {
  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center gap-4 px-6 text-center">
      <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
        Página no encontrada
      </h1>
      <p className="text-slate-600">La dirección a la que entraste no existe.</p>
      <Link to="/">
        <Button variant="secondary">Volver al inicio</Button>
      </Link>
    </div>
  );
}
