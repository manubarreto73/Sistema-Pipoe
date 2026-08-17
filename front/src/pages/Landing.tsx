import { Link } from "react-router";

import fotoArlette from "@/assets/foto_arlette.jpeg";
import { Button } from "@/components/ui/Button";
import { HtmlSeguro } from "@/components/ui/HtmlSeguro";
import { Spinner } from "@/components/ui/Spinner";
import { useTextosLanding } from "@/features/landing/hooks";
import { textoDe } from "@/features/landing/types";
import { cn } from "@/lib/cn";

/**
 * Portada pública.
 *
 * Ni una palabra sobre el Modelo PipoE se escribe acá: los textos son de Arlette, viven en la
 * base y los edita ella desde /admin/landing. La transcripción original está en
 * `docs/textos-arlette.md`. Esta pantalla sólo pone el armado de la página.
 */
export default function Landing() {
  const textos = useTextosLanding();

  const contenido = textos.data;
  const de = (clave: Parameters<typeof textoDe>[1]) => textoDe(contenido, clave);

  if (textos.isPending) {
    return (
      <div className="mx-auto flex max-w-3xl items-center gap-2 px-6 py-20 text-slate-500">
        <Spinner />
        <span>Cargando…</span>
      </div>
    );
  }

  if (textos.isError) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-20">
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          No se pudo cargar el contenido de la página. {textos.error.message}
        </p>
      </div>
    );
  }

  return (
    <>
      {/* Portada. El azul institucional a sangre completa es lo primero que se ve. */}
      <section className="bg-brand-600 text-white">
        <div className="mx-auto max-w-3xl px-6 py-20 sm:py-28">
          <h1 className="font-serif text-5xl leading-tight font-bold sm:text-6xl">
            {de("HERO_TITULO")}
          </h1>
          <p className="mt-4 font-serif text-xl text-brand-100 italic sm:text-2xl">
            {de("HERO_SUBTITULO")}
          </p>

          <div className="mt-10 flex flex-wrap gap-3">
            <Link to="/pedir-acceso">
              <Button className="bg-white text-brand-700 hover:bg-brand-50">
                Pedir acceso
              </Button>
            </Link>
            <Link to="/login">
              <Button
                variant="ghost"
                className="text-white ring-1 ring-white/40 ring-inset hover:bg-white/10"
              >
                Ya tengo cuenta
              </Button>
            </Link>
          </div>
        </div>
      </section>

      <Seccion>
        <Titulo pequenio>{de("DESCRIPCION_TITULO")}</Titulo>
        <Cuerpo html={de("DESCRIPCION_CUERPO")} className="mt-5" />
      </Seccion>

      <Seccion tenue>
        <Titulo>{de("MODELO_TITULO")}</Titulo>
        <Cuerpo html={de("MODELO_CUERPO")} className="mt-5" />
      </Seccion>

      <Seccion>
        <Titulo>{de("BIOGRAFIA_TITULO")}</Titulo>

        {/* La foto flota para que el texto la rodee: así el largo de la biografía puede
            cambiar sin que quede un hueco al costado de la imagen. */}
        <img
          src={fotoArlette}
          alt="Retrato de Arlette Pichardo Muñiz"
          width={494}
          height={640}
          className="mt-6 mb-4 w-32 rounded-2xl object-cover shadow-md ring-1 ring-slate-900/5 sm:float-left sm:mr-7 sm:w-48"
        />

        <Cuerpo html={de("BIOGRAFIA_CUERPO")} className="mt-5" />
        <div aria-hidden className="clear-both" />

        <a
          href="https://arlettepichardo.com/"
          target="_blank"
          rel="noreferrer"
          className="mt-8 inline-flex items-center gap-1.5 rounded-lg border border-brand-200 bg-brand-50 px-4 py-2.5 text-sm font-medium text-brand-700 transition-colors hover:bg-brand-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
        >
          Visitar arlettepichardo.com
          <span aria-hidden>↗</span>
        </a>
      </Seccion>

      {/* Cierre: repite la invitación para quien llegó leyendo hasta el final. */}
      <section className="border-t border-slate-200 bg-brand-50">
        <div className="mx-auto flex max-w-3xl flex-col items-start gap-5 px-6 py-14">
          <h2 className="font-serif text-2xl font-bold text-brand-900">
            Trabajar con el Modelo PipoE
          </h2>
          <div className="flex flex-wrap gap-3">
            <Link to="/pedir-acceso">
              <Button>Pedir acceso</Button>
            </Link>
            <Link to="/login">
              <Button variant="secondary">Ya tengo cuenta</Button>
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}

/** Una sección de la portada. `tenue` alterna el fondo para que el scroll tenga ritmo. */
function Seccion({ tenue, children }: { tenue?: boolean; children: React.ReactNode }) {
  return (
    <section className={cn("border-t border-slate-200", tenue ? "bg-slate-50" : "bg-white")}>
      <div className="mx-auto max-w-3xl px-6 py-14 sm:py-16">{children}</div>
    </section>
  );
}

function Titulo({ pequenio, children }: { pequenio?: boolean; children: React.ReactNode }) {
  if (pequenio)
    return (
      <h2 className="text-sm font-bold tracking-widest text-brand-600 uppercase">
        {children}
      </h2>
    );

  return (
    <h2 className="font-serif text-3xl font-bold text-slate-900">
      {children}
      <span aria-hidden className="mt-3 block h-1 w-16 rounded-full bg-acento-rojo" />
    </h2>
  );
}

/** El HTML lo escribió la cuenta de administradora con el editor de la app. */
function Cuerpo({ html, className }: { html: string; className?: string }) {
  if (!html) return null;

  return <HtmlSeguro html={html} className={cn("prose-landing text-slate-700", className)} />;
}
