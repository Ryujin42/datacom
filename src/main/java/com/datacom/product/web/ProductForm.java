package com.datacom.product.web;

import com.datacom.product.domain.Product;

public record ProductForm(
        Long id,
        long version,
        int currentStep,
        boolean complete,
        String status,
        String reference,
        String name,
        String description,
        String category,
        String subcategory,
        String manufacturer,
        String country,
        String lotNumber,
        String certification,
        String authorComment) {

    public static ProductForm of(Product product) {
        return new ProductForm(
                product.getId(),
                product.getVersion(),
                product.getCurrentStep(),
                product.isComplete(),
                product.getStatus().name(),
                blankIfNull(product.getReference()),
                blankIfNull(product.getName()),
                blankIfNull(product.getDescription()),
                blankIfNull(product.getCategory()),
                blankIfNull(product.getSubcategory()),
                blankIfNull(product.getManufacturer()),
                blankIfNull(product.getCountry()),
                blankIfNull(product.getLotNumber()),
                blankIfNull(product.getCertification()),
                blankIfNull(product.getAuthorComment()));
    }

    private static String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}
