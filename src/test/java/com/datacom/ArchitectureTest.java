package com.datacom;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

class ArchitectureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.datacom");

    @Test
    void domainMustNotDependOnAnyFramework() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "org.springframework..", "jakarta.servlet..", "org.hibernate..")
                        .allowEmptyShould(true);

        rule.check(CLASSES);
    }

    @Test
    void presentationMustNotAccessRepositoriesOrPersistenceApiDirectly() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..web..")
                        .should()
                        .dependOnClassesThat(
                                resideInAnyPackage("jakarta.persistence..")
                                        .or(assignableTo(Repository.class)))
                        .allowEmptyShould(true);

        rule.check(CLASSES);
    }

    @Test
    void applicationMustNotDependOnPresentation() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..web..")
                        .allowEmptyShould(true);

        rule.check(CLASSES);
    }

    @Test
    void domainClassesShouldOnlyBeUsedThroughApplicationOrInfrastructure() {
        // Repere de coherence, pas une regle stricte : le domaine se trouve toujours au
        // meme niveau de decoupage (domaine fonctionnel puis couche), jamais a la racine.
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .resideInAnyPackage("com.datacom.*.domain..")
                        .allowEmptyShould(true);

        rule.check(CLASSES);
    }
}
