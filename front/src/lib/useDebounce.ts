import { useEffect, useState } from "react";

/**
 * Devuelve el valor recién cuando dejó de cambiar por `ms`.
 *
 * Para una caja de búsqueda que consulta la API: sin esto, escribir "Arlette" dispara siete
 * requests, seis de las cuales ya no le importan a nadie.
 */
export function useDebounce<T>(valor: T, ms = 350): T {
  const [retrasado, setRetrasado] = useState(valor);

  useEffect(() => {
    const timer = setTimeout(() => setRetrasado(valor), ms);
    return () => clearTimeout(timer);
  }, [valor, ms]);

  return retrasado;
}
