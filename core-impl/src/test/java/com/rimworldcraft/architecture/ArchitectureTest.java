package com.rimworldcraft.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;

/** Executable architecture baseline for the active platform-neutral modules. */
@AnalyzeClasses(packages = "com.rimworldcraft")
@Tag("architecture")
class ArchitectureTest {
    private static final String CORE = "com.rimworldcraft.core..";
    private static final String PLATFORM = "net.minecraft..";

    @ArchTest
    static final ArchRule core_must_not_depend_on_platform_or_outer_layers = noClasses()
            .that().resideInAnyPackage(CORE)
            .should().dependOnClassesThat().resideInAnyPackage(
                    PLATFORM,
                    "net.minecraftforge..",
                    "net.fabricmc..",
                    "baritone..",
                    "com.rimworldcraft.infrastructure..",
                    "com.rimworldcraft.client..");

    @ArchTest
    static final ArchRule ports_must_be_interfaces = classes()
            .that().resideInAnyPackage("com.rimworldcraft.core.ports..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule core_events_must_not_depend_on_repositories_or_outer_layers = noClasses()
            .that().resideInAnyPackage("com.rimworldcraft.core.events..", "com.rimworldcraft.core..event..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.rimworldcraft.core..repository..",
                    "com.rimworldcraft.infrastructure..",
                    "com.rimworldcraft.client..");

    @ArchTest
    static final ArchRule shared_kernel_must_not_depend_on_contexts = noClasses()
            .that().resideInAnyPackage("com.rimworldcraft.core.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.rimworldcraft.core.colony..",
                    "com.rimworldcraft.core.npc..",
                    "com.rimworldcraft.core.storyteller..",
                    "com.rimworldcraft.core.world..",
                    "com.rimworldcraft.core.player..",
                    "com.rimworldcraft.core.goal..",
                    "com.rimworldcraft.core.building..");

    @ArchTest
    static final ArchRule core_must_not_have_public_setters = noMethods()
            .that().arePublic()
            .and().haveNameMatching("set[A-Z].*")
            .and().areDeclaredInClassesThat().resideInAnyPackage(CORE)
            .should().exist();

    @ArchTest
    static final ArchRule no_public_static_mutable_fields = noFields()
            .that().arePublic()
            .and().areStatic()
            .and().areNotFinal()
            .should().exist();

    @ArchTest
    static final ArchRule core_slices_must_be_free_of_cycles = slices()
            .matching("com.rimworldcraft.core.(*)..")
            .should().beFreeOfCycles();
}
