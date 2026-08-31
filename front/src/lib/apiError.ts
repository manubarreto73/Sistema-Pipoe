/** Forma del body de error que devuelve GlobalExceptionHandler de la API. */
export type ApiErrorBody = {
  status: number;
  /** String en la mayoría de los casos; un mapa campo → mensaje en errores de validación. */
  message: unknown;
  timestamp: string;
};

/** Error normalizado de la API. `status: 0` significa que la request nunca llegó. */
export class ApiError extends Error {
  readonly status: number;
  /** Errores por campo cuando la API responde 400 por validación de DTO. */
  readonly fieldErrors: Record<string, string> | null;

  constructor(
    status: number,
    message: string,
    fieldErrors: Record<string, string> | null = null,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/**
 * Todo lo que sale de apiFetch es un ApiError, así que se lo declaramos a react-query una vez
 * y `query.error` / `mutation.error` quedan tipados en toda la app. Sin esto vienen como
 * `Error` y no se puede mirar `status` (por ejemplo, el 409 del editor) sin castear.
 */
declare module "@tanstack/react-query" {
  interface Register {
    defaultError: ApiError;
  }
}

/**
 * Traduce el body de error de la API a un ApiError.
 *
 * `message` viene tipado como Object en Java: para BusinessException es un string,
 * pero para errores de validación es un Map<String, String> con un mensaje por campo.
 */
export function toApiError(status: number, body: unknown): ApiError {
  const message = (body as ApiErrorBody | null)?.message;

  if (message && typeof message === "object") {
    const fieldErrors = message as Record<string, string>;
    const first = Object.values(fieldErrors)[0];
    return new ApiError(status, first ?? "Revisa los datos ingresados", fieldErrors);
  }

  if (typeof message === "string" && message.length > 0) {
    return new ApiError(status, message);
  }

  return new ApiError(status, sinCuerpo(status));
}

/**
 * Qué decir cuando la respuesta no trae mensaje propio.
 *
 * Pasa siempre que el error no lo genera la API: un 502 o un 503 los escribe nginx, con una
 * página HTML que no tiene nuestro JSON. Sin esto la pantalla mostraba "Error 502", que no le
 * dice nada a nadie y sugiere que la persona hizo algo mal cuando el problema es del servidor.
 */
function sinCuerpo(status: number): string {
  if (status === 429)
    return "Demasiados intentos. Espera unos minutos y vuelve a intentarlo.";

  // 502 y 504 aparecen sobre todo mientras la API se reinicia después de una actualización.
  if (status === 502 || status === 503 || status === 504)
    return "El servidor no está respondiendo. Puede estar reiniciándose: espera un minuto y vuelve a intentarlo.";

  if (status >= 500)
    return "Hubo un problema en el servidor. Vuelve a intentarlo en un momento.";

  return `Error ${status}`;
}
