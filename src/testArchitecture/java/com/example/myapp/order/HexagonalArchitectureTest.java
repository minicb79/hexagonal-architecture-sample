package com.example.myapp.order;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Architectural compliance test suite enforcing Hexagonal package boundaries.
 */
@AnalyzeClasses(packages = {
        "com.example.myapp.order.domain",
        "com.example.myapp.order.application",
        "com.example.myapp.order.adapter"
})
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule hexagonal_layers_are_respected = onionArchitecture()
            .domainModels("com.example.myapp.order.domain.model..", "com.example.myapp.order.domain.exception..")
            .domainServices("com.example.myapp.order.domain.service..")
            .applicationServices("com.example.myapp.order.application..")
            .adapter("rest", "com.example.myapp.order.adapter.in.rest..")
            .adapter("cardsservice", "com.example.myapp.order.adapter.out.cardsservice..")
            .adapter("paymentsservice", "com.example.myapp.order.adapter.out.paymentsservice..")
            .withOptionalLayers(true);

    @ArchTest
    public static final ArchRule domain_must_be_framework_free = noClasses()
            .that().resideInAPackage("..order.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "javax.persistence..",
                    "com.fasterxml.jackson.."
            );

    @ArchTest
    public static final ArchRule application_must_not_depend_on_adapters = noClasses()
            .that().resideInAPackage("..order.application..")
            .should().dependOnClassesThat().resideInAPackage("..order.adapter..");

    @ArchTest
    public static final ArchRule card_models_are_private_to_cards_service = classes()
            .that().resideInAPackage("..adapter.out.cardsservice.model..")
            .should().onlyBeAccessed().byAnyPackage("..adapter.out.cardsservice..");

    @ArchTest
    public static final ArchRule payment_models_are_private_to_payments_service = classes()
            .that().resideInAPackage("..adapter.out.paymentsservice.model..")
            .should().onlyBeAccessed().byAnyPackage("..adapter.out.paymentsservice..");
}
