package com.example.app.model.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> data,
        PaginationMeta pagination
) {
    public static <T> PagedResponse<T> of(List<T> data, int page, int size, long totalElements) {
        return new PagedResponse<>(data, PaginationMeta.of(page, size, totalElements));
    }
}
