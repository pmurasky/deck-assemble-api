package com.deckassemble;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

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

    @ArchTest
    static final ArchRule application_does_not_depend_outward =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..api..", "..infrastructure..")
                    .because("application services define ports; adapters live in infrastructure");

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

    // shared is the DDD shared kernel (ApiExceptionHandler, CurrentUser) — every context may
    // depend on it and it may reference contexts, so it is excluded from cycle detection.
    @ArchTest
    static final ArchRule bounded_contexts_are_cycle_free =
            SlicesRuleDefinition.slices()
                    .matching(
                            "com.deckassemble.(cards|decks|collections|imports|users|administration|authentication)..")
                    .should()
                    .beFreeOfCycles()
                    .because(
                            "cyclic dependencies between bounded contexts block independent change");

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
