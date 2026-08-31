import type { ComponentType } from "react";

const MARCA = "pipoe:recarga-por-version";

/**
 * Carga diferida de una pantalla, tolerante a que la aplicación se haya actualizado mientras
 * la pestaña estaba abierta.
 *
 * Cada pantalla privada viaja en su propio archivo con un hash en el nombre. Al desplegar una
 * versión nueva esos archivos cambian de nombre y los anteriores dejan de existir, así que una
 * pestaña abierta desde antes sigue pidiendo los viejos y recibe un 404 disfrazado de
 * "Failed to fetch dynamically imported module".
 *
 * No es un error del que se pueda volver reintentando: el índice que tiene cargado esa pestaña
 * ya no describe lo que hay en el servidor. Lo único que lo arregla es recargar, que trae un
 * index.html nuevo — que se sirve con `no-cache` justamente para esto.
 *
 * La marca en sessionStorage corta el bucle: si después de recargar vuelve a fallar, el
 * problema es otro (la red, un archivo que no se subió) y conviene que el error se vea.
 */
export function pagina(importar: () => Promise<{ default: ComponentType }>) {
  return async () => {
    try {
      const modulo = await importar();
      olvidar();
      return { Component: modulo.default };
    } catch (error) {
      if (yaRecargue()) throw error;

      recordar();
      window.location.reload();

      // La recarga no es instantánea. Esta promesa no se resuelve nunca a propósito: si
      // devolviera algo, el router seguiría adelante y pintaría una pantalla de error que
      // está por desaparecer igual.
      return new Promise<{ Component: ComponentType }>(() => {});
    }
  };
}

// sessionStorage puede tirar excepción —modo privado, cookies bloqueadas—, y quedarse sin la
// marca es peor que no tenerla: se recargaría en bucle. Ante la duda, decimos que ya se
// recargó, que es el lado seguro: se ve el error en vez de un ciclo infinito.

function yaRecargue() {
  try {
    return sessionStorage.getItem(MARCA) !== null;
  } catch {
    return true;
  }
}

function recordar() {
  try {
    sessionStorage.setItem(MARCA, "1");
  } catch {
    // Sin almacenamiento no hay red de contención posible; se recarga igual, una sola vez,
    // porque el navegador que no guarda esto tampoco va a volver a entrar acá en un bucle
    // rápido: la recarga descarta el estado de la página.
  }
}

function olvidar() {
  try {
    sessionStorage.removeItem(MARCA);
  } catch {
    // Nada que limpiar si nunca se pudo escribir.
  }
}
