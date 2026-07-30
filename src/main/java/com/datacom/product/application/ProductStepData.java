package com.datacom.product.application;

public final class ProductStepData {

    private ProductStepData() {}

    /** Etape 1 — Identification. */
    public record Identification(String reference, String name, String description) {}

    /** Etape 2 — Classification. */
    public record Classification(
            String category, String subcategory, String manufacturer, String country) {}

    /** Etape 3 — Tracabilite et conformite. */
    public record Traceability(String lotNumber, String certification, String authorComment) {}
}
