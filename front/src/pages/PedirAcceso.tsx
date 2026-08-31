import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm, useWatch } from "react-hook-form";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { CheckboxGrupo, RadioGrupo } from "@/components/ui/OpcionesGrupo";
import { Select } from "@/components/ui/Select";
import { Textarea } from "@/components/ui/Textarea";
import { useCrearSolicitud } from "@/features/solicitudes/hooks";
import { PAISES } from "@/features/solicitudes/paises";
import {
  nuevaSolicitudSchema,
  type NuevaSolicitudValues,
} from "@/features/solicitudes/schemas";
import {
  CANALES_DIFUSION,
  GENEROS,
  NIVELES_INSTRUCCION,
  OCUPACIONES,
  RANGOS_EDAD,
  USOS_PREVISTOS,
} from "@/features/solicitudes/types";

export default function PedirAcceso() {
  const solicitud = useCrearSolicitud();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<NuevaSolicitudValues>({
    resolver: zodResolver(nuevaSolicitudSchema),
    defaultValues: {
      nombre: "",
      apellidos: "",
      email: "",
      ocupacionOtra: "",
      institucion: "",
      paisNacimiento: "",
      paisResidencia: "",
      motivacion: "",
      usos: [],
      usosOtro: "",
      canales: [],
      canalOtro: "",
    },
  });

  // useWatch y no watch(): sólo re-renderiza por estos tres campos, y es la API que el
  // compilador de React sabe memoizar.
  const ocupacion = useWatch({ control, name: "ocupacion" });
  const usos = useWatch({ control, name: "usos" });
  const canales = useWatch({ control, name: "canales" });

  if (solicitud.isSuccess) {
    return (
      <div className="mx-auto max-w-md px-6 py-16">
        <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
          Solicitud enviada
        </h1>
        <p className="mt-4 text-slate-600">
          Vamos a revisarla y, si se aprueba, vas a recibir un mail con una contraseña
          temporal para entrar. Después vas a poder cambiarla.
        </p>
        <Link to="/" className="mt-6 inline-block text-sm text-brand-700 hover:underline">
          Volver al inicio
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-16">
      <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">Pedir acceso</h1>
      <p className="mt-2 text-sm text-slate-600">
        Cuéntanos quién eres y qué uso piensas darle al Modelo PipoE. Si se aprueba tu
        solicitud, vas a recibir tus datos de acceso por mail.
      </p>

      <form
        noValidate
        onSubmit={handleSubmit((values) => solicitud.mutate(values))}
        className="mt-10 flex flex-col gap-10"
      >
        <section className="flex flex-col gap-4">
          <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">
            Datos personales
          </h2>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Nombre" htmlFor="nombre" error={errors.nombre?.message}>
              <Input
                id="nombre"
                autoComplete="given-name"
                invalid={Boolean(errors.nombre)}
                {...register("nombre")}
              />
            </Field>

            <Field label="Apellidos" htmlFor="apellidos" error={errors.apellidos?.message}>
              <Input
                id="apellidos"
                autoComplete="family-name"
                invalid={Boolean(errors.apellidos)}
                {...register("apellidos")}
              />
            </Field>
          </div>

          <Field label="Correo electrónico" htmlFor="email" error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              invalid={Boolean(errors.email)}
              {...register("email")}
            />
          </Field>

          <Controller
            control={control}
            name="nivelInstruccion"
            render={({ field }) => (
              <RadioGrupo
                label="Nivel de instrucción"
                opciones={NIVELES_INSTRUCCION}
                valor={field.value}
                onChange={field.onChange}
                error={errors.nivelInstruccion?.message}
              />
            )}
          />

          <Controller
            control={control}
            name="genero"
            render={({ field }) => (
              <RadioGrupo
                label="Género"
                opciones={GENEROS}
                valor={field.value}
                onChange={field.onChange}
                error={errors.genero?.message}
              />
            )}
          />

          <Controller
            control={control}
            name="rangoEdad"
            render={({ field }) => (
              <RadioGrupo
                label="Edad (en años cumplidos)"
                opciones={RANGOS_EDAD}
                valor={field.value}
                onChange={field.onChange}
                error={errors.rangoEdad?.message}
              />
            )}
          />
        </section>

        <section className="flex flex-col gap-4 border-t border-slate-200 pt-8">
          <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">
            Ocupación y procedencia
          </h2>

          <Controller
            control={control}
            name="ocupacion"
            render={({ field }) => (
              <RadioGrupo
                label="Ocupación principal"
                opciones={OCUPACIONES}
                valor={field.value}
                onChange={field.onChange}
                error={errors.ocupacion?.message}
              >
                {ocupacion === "OTRA" && (
                  <Field
                    label="Especifica cuál"
                    htmlFor="ocupacionOtra"
                    error={errors.ocupacionOtra?.message}
                  >
                    <Input
                      id="ocupacionOtra"
                      invalid={Boolean(errors.ocupacionOtra)}
                      {...register("ocupacionOtra")}
                    />
                  </Field>
                )}
              </RadioGrupo>
            )}
          />

          <Field
            label="Institución u organización a la que pertenece"
            htmlFor="institucion"
            error={errors.institucion?.message}
          >
            <Input
              id="institucion"
              placeholder="Nombre completo y país"
              invalid={Boolean(errors.institucion)}
              {...register("institucion")}
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label="País de nacimiento"
              htmlFor="paisNacimiento"
              error={errors.paisNacimiento?.message}
            >
              <Select id="paisNacimiento" className="w-full" {...register("paisNacimiento")}>
                <option value="">Elige un país…</option>
                {PAISES.map((pais) => (
                  <option key={pais} value={pais}>
                    {pais}
                  </option>
                ))}
              </Select>
            </Field>

            <Field
              label="País de residencia"
              htmlFor="paisResidencia"
              error={errors.paisResidencia?.message}
            >
              <Select id="paisResidencia" className="w-full" {...register("paisResidencia")}>
                <option value="">Sólo si es distinto…</option>
                {PAISES.map((pais) => (
                  <option key={pais} value={pais}>
                    {pais}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
        </section>

        <section className="flex flex-col gap-4 border-t border-slate-200 pt-8">
          <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">
            Sobre el Modelo PipoE
          </h2>

          <Field
            label="¿Por qué le interesa el Modelo PipoE?"
            htmlFor="motivacion"
            error={errors.motivacion?.message}
          >
            <Textarea
              id="motivacion"
              rows={4}
              maxLength={1000}
              invalid={Boolean(errors.motivacion)}
              {...register("motivacion")}
            />
          </Field>

          <Controller
            control={control}
            name="usos"
            render={({ field }) => (
              <CheckboxGrupo
                label="¿Cuáles son los usos que le dará al Modelo PipoE?"
                ayuda="puede marcar varias opciones"
                opciones={USOS_PREVISTOS}
                valores={field.value}
                onChange={field.onChange}
                error={errors.usos?.message}
              >
                {usos?.includes("OTRO") && (
                  <Field
                    label="Especifica cuál"
                    htmlFor="usosOtro"
                    error={errors.usosOtro?.message}
                  >
                    <Input
                      id="usosOtro"
                      invalid={Boolean(errors.usosOtro)}
                      {...register("usosOtro")}
                    />
                  </Field>
                )}
              </CheckboxGrupo>
            )}
          />

          <Controller
            control={control}
            name="canales"
            render={({ field }) => (
              <CheckboxGrupo
                label="¿Cómo se enteró del Modelo PipoE?"
                ayuda="puede marcar varias opciones"
                opciones={CANALES_DIFUSION}
                valores={field.value}
                onChange={field.onChange}
                error={errors.canales?.message}
              >
                {canales?.includes("OTRO") && (
                  <Field
                    label="Especifica cuál"
                    htmlFor="canalOtro"
                    error={errors.canalOtro?.message}
                  >
                    <Input
                      id="canalOtro"
                      invalid={Boolean(errors.canalOtro)}
                      {...register("canalOtro")}
                    />
                  </Field>
                )}
              </CheckboxGrupo>
            )}
          />
        </section>

        {solicitud.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {solicitud.error.message}
          </p>
        )}

        <Button type="submit" loading={solicitud.isPending}>
          Enviar solicitud
        </Button>
      </form>
    </div>
  );
}
