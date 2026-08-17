/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base de la API de Pipoe, sin barra final. Ej: http://localhost:8080 */
  readonly VITE_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
