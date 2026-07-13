package com.example.dawanow.dtos.response;

import java.util.List;
import org.springframework.data.domain.Pageable;

public record PaginatedResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PaginatedResponse<T> empty(Pageable pageable) {
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int pageSize = pageable.isPaged() ? pageable.getPageSize() : 0;

        return new PaginatedResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
    }
}
