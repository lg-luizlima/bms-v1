package br.com.tlf.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "br.com.tlf", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("api").definedBy("br.com.tlf.api..")
            .layer("core").definedBy("br.com.tlf.core..")
            .layer("infrastructure").definedBy("br.com.tlf.infrastructure..")
            .layer("shared").definedBy("br.com.tlf.shared..")
            .whereLayer("api").mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("core").mayOnlyBeAccessedByLayers("api", "infrastructure", "shared");

    @ArchTest
    static final ArchRule coreIsFreeOfSerializationConcerns = noClasses()
            .that().resideInAPackage("br.com.tlf.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.fasterxml.jackson..",
                    "io.swagger..",
                    "jakarta.validation..")
            .because("request/response shape and JSON encoding belong to the adapters, not the domain");

    @ArchTest
    static final ArchRule coreIsFreeOfInfrastructureClients = noClasses()
            .that().resideInAPackage("br.com.tlf.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data..",
                    "org.springframework.transaction..",
                    "org.springframework.web..",
                    "jakarta.persistence..",
                    "com.azure..",
                    "io.micrometer..")
            .because("Redis, JPA, transactions, HTTP and tracing reach the core only through out-ports");

    @ArchTest
    static final ArchRule domainDoesNotDependOnSpring = noClasses()
            .that().resideInAPackage("br.com.tlf.core.domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .because("the domain model must be constructible without a container");

    @ArchTest
    static final ArchRule useCasesImplementInboundPorts = com.tngtech.archunit.lang.syntax.ArchRuleDefinition
            .classes()
            .that().resideInAPackage("br.com.tlf.core.application.usecase..")
            .and().haveSimpleNameEndingWith("UseCase")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "an inbound port", javaClass -> javaClass.getPackageName().equals("br.com.tlf.core.port.in")))
            .because("every use case is reachable only through the port it fulfils");

    @ArchTest
    static final ArchRule jpaEntitiesStayInPersistence = noClasses()
            .that().resideOutsideOfPackage("br.com.tlf.infrastructure.persistence..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("JpaEntity")
            .because("entities are a storage detail and must not leak past the repository adapters");
}
