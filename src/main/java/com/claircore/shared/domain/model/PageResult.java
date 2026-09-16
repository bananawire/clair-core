package com.claircore.shared.domain.model;

import java.util.List;

/**
 * Framework-free page of results. Replaces {@code org.springframework.data.domain.Page} in
 * repository ports, query services and query records so that no domain or application signature
 * depends on Spring Data.
 *
 * @param items the items on this page
 * @param page  zero-based page index
 * @param size  requested page size
 * @param total total number of matching items across all pages
 * @param <T>   the item type
 */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public PageResult {
        if (items == null) throw new IllegalArgumentException("items is required");
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
        if (total < 0) throw new IllegalArgumentException("total must not be negative");
        items = List.copyOf(items);
    }

    /** Number of pages available for the current page size. */
    public int totalPages() {
        return (int) Math.ceil((double) total / (double) size);
    }

    /** Empty page for the given pagination request. */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0L);
    }
}
