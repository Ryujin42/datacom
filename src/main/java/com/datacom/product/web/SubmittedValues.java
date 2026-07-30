package com.datacom.product.web;

final class SubmittedValues {

    private SubmittedValues() {}

    static ProductForm merge(ProductForm saved, StepSubmission submission, int step) {
        return switch (step) {
            case 1 ->
                    new ProductForm(
                            saved.id(),
                            saved.version(),
                            saved.currentStep(),
                            saved.complete(),
                            saved.status(),
                            orSaved(submission.reference(), saved.reference()),
                            orSaved(submission.name(), saved.name()),
                            orSaved(submission.description(), saved.description()),
                            saved.category(),
                            saved.subcategory(),
                            saved.manufacturer(),
                            saved.country(),
                            saved.lotNumber(),
                            saved.certification(),
                            saved.authorComment());
            case 2 ->
                    new ProductForm(
                            saved.id(),
                            saved.version(),
                            saved.currentStep(),
                            saved.complete(),
                            saved.status(),
                            saved.reference(),
                            saved.name(),
                            saved.description(),
                            orSaved(submission.category(), saved.category()),
                            orSaved(submission.subcategory(), saved.subcategory()),
                            orSaved(submission.manufacturer(), saved.manufacturer()),
                            orSaved(submission.country(), saved.country()),
                            saved.lotNumber(),
                            saved.certification(),
                            saved.authorComment());
            case 3 ->
                    new ProductForm(
                            saved.id(),
                            saved.version(),
                            saved.currentStep(),
                            saved.complete(),
                            saved.status(),
                            saved.reference(),
                            saved.name(),
                            saved.description(),
                            saved.category(),
                            saved.subcategory(),
                            saved.manufacturer(),
                            saved.country(),
                            orSaved(submission.lotNumber(), saved.lotNumber()),
                            orSaved(submission.certification(), saved.certification()),
                            orSaved(submission.authorComment(), saved.authorComment()));
            default -> saved;
        };
    }

    private static String orSaved(String submitted, String saved) {
        return submitted == null ? saved : submitted;
    }
}
