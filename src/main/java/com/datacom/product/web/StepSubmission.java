package com.datacom.product.web;

public record StepSubmission(
        long version,
        Integer cible,
        String reference,
        String name,
        String description,
        String category,
        String subcategory,
        String manufacturer,
        String country,
        String lotNumber,
        String certification,
        String authorComment) {}
