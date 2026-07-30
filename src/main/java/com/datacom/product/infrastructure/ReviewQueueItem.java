package com.datacom.product.infrastructure;

import java.time.Instant;

public record ReviewQueueItem(
        Long id,
        String reference,
        String name,
        String manufacturer,
        String author,
        Instant submittedAt) {}
