/**
 * Forma con la que Spring Data serializa un `Page<T>`.
 * `number` es la página actual y arranca en 0.
 */
export type Page<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};
