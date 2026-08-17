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
    return new ApiError(status, first ?? "Revisá los datos ingresados", fieldErrors);
  }

  if (typeof message === "string" && message.length > 0) {
    return new ApiError(status, message);
  }

  return new ApiError(status, `Error ${status}`);
}
