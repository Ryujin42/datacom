package com.datacom.product.application;

public final class ProductStepData {

    private ProductStepData() {}

    public record Identification(String reference, String name, String description) {}

    public record Classification(
            String category, String subcategory, String manufacturer, String country) {}

    public record Traceability(String lotNumber, String certification, String authorComment) {}
}
