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

/**
 * Fait respecter mecaniquement TEC-01/02/03/04 (architecture en couches) des le premier lot, avant
 * que le domaine et l'application n'existent : ces regles echouent des qu'une classe les enfreint,
 * plutot que de laisser la derive s'accumuler jusqu'a une revue de code qui arrive trop tard (c'est
 * exactement ce que l'audit reproche au legacy - AUDIT.md S3.1, absence totale de couches).
 *
 * <p>allowEmptyShould(true) : les couches domain/application n'existent pas encore a ce stade (L0)
 * ; les regles doivent passer a vide plutot qu'echouer tant qu'il n'y a rien a verifier, et
 * commencer a s'appliquer des qu'une premiere classe apparait dans le paquet concerne.
 *
 * <p>Decision d'architecture (L1) : jakarta.persistence est toleree dans le domaine. Les
 * annotations JPA (@Entity, @Column...) sont des metadonnees declaratives, pas un couplage
 * comportemental a un framework - c'est le compromis standard d'une architecture en couches Spring
 * (par opposition a un modele hexagonal pur avec mapping domaine/persistence separe,
 * disproportionne pour ce projet). Ce que TEC-01 interdit reste interdit : injection Spring,
 * logique Hibernate specifique, API servlet.
 */
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
        // Cible les depots (Repository) et l'API JPA, pas tout ..infrastructure.. sans
        // distinction : les adaptateurs Spring Security comme UserPrincipal sont concus pour
        // franchir cette frontiere (c'est l'idiome @AuthenticationPrincipal), a la difference
        // d'un Repository ou d'une entite JPA qu'un controleur ne doit jamais manipuler
        // directement (TEC-03, corrige le ResultSet transmis a la JSP du legacy).
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
