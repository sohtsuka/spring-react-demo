package com.example.app.model.dto;

public record PaginationMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static PaginationMeta of(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PaginationMeta(page, size, totalElements, totalPages);
    }
}
