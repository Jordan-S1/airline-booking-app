/** Mirrors com.airlinebookingsystem.dto.common.PagedResponse. */
export interface PagedResponseDto<T> {
  content: T[];
  /** Zero-based. */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
