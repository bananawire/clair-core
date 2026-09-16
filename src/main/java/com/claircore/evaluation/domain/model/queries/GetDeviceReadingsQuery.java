package com.claircore.evaluation.domain.model.queries;

import java.time.Instant;

public record GetDeviceReadingsQuery(Instant start, Instant end) {
    public GetDeviceReadingsQuery {
        if (start == null) {
            throw new IllegalArgumentException("start must not be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("end must not be null");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }
}
