package com.deckassemble;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(packages = "com.deckassemble", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_independent =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..api..", "..application..", "..infrastructure..")
                    .because("domain is the innermost layer and must not depend outward");

    // ponytail: frozen legacy baseline — services currently inject Spring Data repos and consume
    // API/Scryfall DTOs directly. New violations still fail; unfreeze by extracting ports (#1).
    @ArchTest
    static final ArchRule application_does_not_depend_outward =
            FreezingArchRule.freeze(
                    noClasses()
                            .that()
                            .resideInAPackage("..application..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage("..api..", "..infrastructure..")
                            .because(
                                    "application services define ports; adapters live in infrastructure"));

    @ArchTest
    static final ArchRule infrastructure_does_not_depend_on_api =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..api..")
                    .because("infrastructure adapters must not know the web layer");

    @ArchTest
    static final ArchRule api_does_not_depend_on_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because(
                            "controllers go through application services, never adapters directly");

    // ponytail: frozen — cycles route through the shared kernel (ApiExceptionHandler references
    // every context's exceptions; all contexts use shared CurrentUser) plus cards↔imports.
    @ArchTest
    static final ArchRule bounded_contexts_are_cycle_free =
            FreezingArchRule.freeze(
                    SlicesRuleDefinition.slices()
                            .matching("com.deckassemble.(*)..")
                            .should()
                            .beFreeOfCycles()
                            .because(
                                    "cyclic dependencies between bounded contexts block independent change"));

    @ArchTest
    static final ArchRule no_standard_streams =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.because(
                    "use SLF4J instead of System.out/err");

    @ArchTest
    static final ArchRule no_generic_exceptions =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.because(
                    "throw specific exceptions for meaningful error handling");

    @ArchTest
    static final ArchRule no_field_injection =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.because(
                    "constructor injection keeps dependencies explicit and testable");

    @ArchTest
    static final ArchRule no_java_util_logging =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.because(
                    "use SLF4J as the single logging facade");
}
