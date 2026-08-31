import { ApiError, toApiError } from "@/lib/apiError";
import { refreshAccessToken } from "@/lib/refresh";
import { getAccessToken } from "@/lib/tokens";
import { useAuthStore } from "@/stores/auth";

const BASE_URL = import.meta.env.VITE_API_URL;

/** Mensaje exacto que emite JwtAuthenticationFilter cuando el access token venció. */
const TOKEN_EXPIRADO = "Token expirado";

export type ApiFetchOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** `false` para endpoints públicos (login, refresh). Por defecto manda el Bearer. */
  auth?: boolean;
  signal?: AbortSignal;
};

/**
 * Cliente HTTP de la app. Agrega el token, normaliza errores y renueva la sesión
 * de forma transparente cuando el access token vence.
 */
export function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  return request<T>(path, options, true);
}

async function request<T>(
  path: string,
  options: ApiFetchOptions,
  allowRetry: boolean,
): Promise<T> {
  const { method = "GET", body, auth = true, signal } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";

  if (auth) {
    const token = getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    // fetch solo rechaza por red o CORS; un HTTP 500 resuelve normalmente.
    throw new ApiError(0, "No se pudo conectar con el servidor");
  }

  if (response.status === 401 && auth) {
    const apiError = toApiError(401, await readBody(response));

    // Solo el token vencido es recuperable. "Token revocado" y "Token inválido"
    // significan que renovar no va a servir de nada.
    if (allowRetry && apiError.message === TOKEN_EXPIRADO) {
      try {
        await refreshAccessToken();
      } catch {
        useAuthStore.getState().clear();
        throw new ApiError(401, "La sesión expiró. Inicia sesión de nuevo.");
      }
      // Un único reintento: si vuelve a dar 401, allowRetry en false corta la recursión.
      return request<T>(path, options, false);
    }

    useAuthStore.getState().clear();
    throw apiError;
  }

  if (!response.ok) {
    throw toApiError(response.status, await readBody(response));
  }

  // 204 No Content (logout, change-password) no trae body para parsear.
  if (response.status === 204) return undefined as T;

  return (await response.json()) as T;
}

export type ArchivoDescargado = { blob: Blob; nombre: string };

/**
 * Descarga un archivo de la API.
 *
 * No alcanza con apuntar un `<a href>` al endpoint: la autenticación va por header y el
 * navegador no lo manda en una navegación. Hay que traerlo con fetch y guardarlo a mano.
 */
export function apiDownload(path: string): Promise<ArchivoDescargado> {
  return descargar(path, true);
}

async function descargar(path: string, allowRetry: boolean): Promise<ArchivoDescargado> {
  const headers: Record<string, string> = {};
  const token = getAccessToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, { headers });
  } catch {
    throw new ApiError(0, "No se pudo conectar con el servidor");
  }

  if (response.status === 401) {
    const apiError = toApiError(401, await readBody(response));

    if (allowRetry && apiError.message === TOKEN_EXPIRADO) {
      try {
        await refreshAccessToken();
      } catch {
        useAuthStore.getState().clear();
        throw new ApiError(401, "La sesión expiró. Inicia sesión de nuevo.");
      }
      return descargar(path, false);
    }

    useAuthStore.getState().clear();
    throw apiError;
  }

  if (!response.ok) throw toApiError(response.status, await readBody(response));

  return { blob: await response.blob(), nombre: nombreDelArchivo(response) };
}

/** El nombre que puso la API en Content-Disposition, con los acentos ya decodificados. */
function nombreDelArchivo(response: Response) {
  const cabecera = response.headers.get("Content-Disposition") ?? "";

  const codificado = /filename\*=UTF-8''([^;]+)/i.exec(cabecera);
  if (codificado) return decodeURIComponent(codificado[1]);

  const simple = /filename="?([^";]+)"?/i.exec(cabecera);
  return simple ? simple[1] : "descarga";
}

/** Dispara el "guardar como" del navegador con un blob que ya está en memoria. */
export function guardarArchivo({ blob, nombre }: ArchivoDescargado) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement("a");

  enlace.href = url;
  enlace.download = nombre;
  document.body.append(enlace);
  enlace.click();
  enlace.remove();

  URL.revokeObjectURL(url);
}

async function readBody(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
