/**
 * El access token vive solo en memoria, nunca en localStorage.
 *
 * Es el token que viaja en cada request, así que mantenerlo fuera del disco reduce
 * la ventana de robo por XSS: un script inyectado no lo encuentra leyendo el storage.
 * El costo es que se pierde al recargar la página, y por eso existe el bootstrap de
 * sesión (ver AuthBootstrap): con el refresh token persistido se pide uno nuevo.
 */
let accessToken: string | null = null;

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}
