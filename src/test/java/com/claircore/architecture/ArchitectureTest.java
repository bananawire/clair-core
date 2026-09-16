package com.claircore.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("com.claircore");

    @Test
    void domainIsFrameworkFree() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta..", "org.springframework..", "org.hibernate..", "com.fasterxml..",
                    "..application..", "..infrastructure..", "..interfaces..")
                .check(CLASSES);
    }

    @Test
    void applicationAndInterfacesDoNotDependOnInfrastructure() {
        noClasses().that().resideInAnyPackage("..application..", "..interfaces..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..").check(CLASSES);
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..interfaces.rest..", "jakarta.persistence..")
                .check(CLASSES);
        noClasses().that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat().resideInAPackage("..domain.repositories..").check(CLASSES);
        noClasses().that().resideInAPackage("..application.acl..")
                .should().dependOnClassesThat().resideInAPackage("..domain.repositories..").check(CLASSES);
    }

    @Test
    void contextsCommunicateThroughPublishedContractsAndSharedHasNoContextDependencies() {
        classes().should(new ArchCondition<JavaClass>("use only published contracts across contexts") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String[] sourceParts = source.getPackageName().split("\\.");
                if (sourceParts.length < 3) return;
                String context = sourceParts[2];
                source.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String target = dependency.getTargetClass().getPackageName();
                    if (!target.startsWith("com.claircore.")) return;
                    String other = target.split("\\.")[2];
                    if (context.equals(other) || other.equals("shared")) return;
                    boolean allowed = !context.equals("shared") &&
                            (target.startsWith("com.claircore." + other + ".interfaces.acl") ||
                             target.startsWith("com.claircore." + other + ".interfaces.events"));
                    events.add(new SimpleConditionEvent(dependency, allowed, dependency.getDescription()));
                });
            }
        }).check(CLASSES);
    }

    @Test
    void persistenceAssemblersAreStaticUtilities() {
        constructors().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistenceAssembler")
                .should().bePrivate().check(CLASSES);
        methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistenceAssembler")
                .should().beStatic().check(CLASSES);
    }
}
