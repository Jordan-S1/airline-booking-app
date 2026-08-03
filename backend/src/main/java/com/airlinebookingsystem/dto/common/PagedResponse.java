package com.airlinebookingsystem.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A page of results with the counts a client needs to render controls.
 *
 * <p>Deliberately not Spring's {@code Page}: serialising {@code PageImpl}
 * directly produces JSON whose shape is tied to an implementation detail and
 * which Spring itself warns about, having changed it between versions. Naming
 * the fields here makes the contract ours.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    /** Wraps a repository page, mapping each entity to its response DTO. */
    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}