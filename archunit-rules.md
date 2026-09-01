# RimWorldCraft — ArchUnit Rules

## 1. Введение

ArchUnit — библиотека для архитектурных тестов Java. Она позволяет проверять package boundaries, dependency direction, naming conventions, annotations и relationships между классами непосредственно в CI.

Для RimWorldCraft ArchUnit является автоматической защитой от архитектурного дрейфа. Документы могут описывать, что Core не должен зависеть от Minecraft, но только executable rule гарантирует, что случайный import `net.minecraft` действительно остановит Pull Request.

### 1.1 Цели

- сохранить изоляцию Core от Minecraft/Forge/Fabric;
- защитить bounded-context boundaries;
- закрепить Ports and Adapters;
- контролировать расположение repositories, factories, aggregates и events;
- не допустить forbidden practices;
- сделать архитектурные решения проверяемыми и видимыми.

ArchUnit запускается минимум на каждый PR. Нарушение обязательного правила делает CI красным и блокирует merge.

### 1.2 Граница ответственности ArchUnit

ArchUnit хорошо проверяет structure/dependencies. Не следует заставлять его доказывать то, что лучше проверяется другими инструментами:

| Требование | Основной инструмент |
|---|---|
| package/import/dependency | ArchUnit |
| code formatting | Checkstyle/Spotless/Google Java Format |
| bugs/nullness | SpotBugs/SonarQube |
| line/branch coverage | JaCoCo |
| public method behavior coverage | JUnit + JaCoCo/Mutation testing |
| JSON schemas | JSON Schema validator |
| forbidden `System.out` calls | ArchUnit или Checkstyle |
| NBT migrations | JUnit integration tests |

## 2. Package convention

Целевой namespace:

```text
com.rimworldcraft.core.shared
com.rimworldcraft.core.ports
com.rimworldcraft.core.colony
com.rimworldcraft.core.npc
com.rimworldcraft.core.story
com.rimworldcraft.core.goal
com.rimworldcraft.core.building
com.rimworldcraft.infrastructure.adapter
com.rimworldcraft.infrastructure.repository
com.rimworldcraft.infrastructure.config
com.rimworldcraft.infrastructure.save
com.rimworldcraft.infrastructure.entity
com.rimworldcraft.client
```

В документах проекта встречаются сокращённые формы `core.colony` и `core.story`; в executable rules используется полный namespace. Если кодовая база выберет иной base package, меняется только `ArchitecturePackages`, а не смысл правил.

### 2.1 Centralized package constants

```java
final class ArchitecturePackages {
    static final String CORE = "com.rimworldcraft.core..";
    static final String CORE_SHARED = "com.rimworldcraft.core.shared..";
    static final String CORE_PORTS = "com.rimworldcraft.core.ports..";
    static final String CORE_EVENTS = "com.rimworldcraft.core.events..";
    static final String INFRASTRUCTURE = "com.rimworldcraft.infrastructure..";
    static final String ADAPTERS = "com.rimworldcraft.infrastructure.adapter..";
    static final String CLIENT = "com.rimworldcraft.client..";

    private ArchitecturePackages() {}
}
```

## 3. Импорт классов для архитектурных тестов

### 3.1 Общий importer

```java
@AnalyzeClasses(packages = "com.rimworldcraft")
public class ArchitectureTest {
    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .importPackages("com.rimworldcraft");
}
```

JUnit/ArchUnit integration может использовать `@AnalyzeClasses` и `@ArchTest`, что автоматически импортирует classes один раз. Для test classes используется отдельный importer с test output, если проект действительно хочет проверять test architecture.

### 3.2 Архитектурные predicates

```java
private static final ArchCondition<JavaClass> NOT_MINECRAFT_DEPENDENCY =
        ArchConditions.notDependOnClassesThat()
                .resideInAnyPackage(
                        "net.minecraft..",
                        "net.minecraftforge..",
                        "net.fabricmc..",
                        "baritone..",
                        "automatone..");
```

При выборе predicate важно учитывать imports, method parameter types, annotations, inheritance и field types. `dependOnClassesThat` обычно проверяет все эти зависимости.

## 4. Общие правила проекта

### 4.1 Core не импортирует Minecraft

```java
@ArchTest
static final ArchRule core_must_not_depend_on_minecraft =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "net.minecraft..",
                        "net.minecraftforge..",
                        "net.fabricmc..",
                        "com.mojang.blaze3d..",
                        "baritone..",
                        "automatone..");
```

Это ключевое правило hexagonal architecture. `core` может использовать `Position`, `GridPosition`, `WorldId`, но не `BlockPos`, `Level`, `Entity`, `ItemStack`, Baritone `Goal` или loader events.

### 4.2 Запрет `System.out`, `System.err` и stack trace

```java
@ArchTest
static final ArchRule production_must_not_write_to_console =
        noClasses()
                .that().resideOutsideOfPackages("..test..", "..tests..")
                .should().accessClassesThat()
                .belongToAnyOf(System.class);
```

Если broad rule вызывает false positives в generated/bootstrap code, ограничить packages и дополнить Checkstyle. Для `printStackTrace()` можно использовать:

```java
@ArchTest
static final ArchRule production_must_not_call_print_stack_trace =
        noClasses()
                .that().resideOutsideOfPackages("..test..", "..tests..")
                .should().callMethod(Throwable.class, "printStackTrace");
```

В production используется project logger/observability port.

### 4.3 Public static fields

Полностью запретить `public static final` константы было бы слишком строго. Запрещаем public static mutable fields:

```java
@ArchTest
static final ArchRule no_public_static_mutable_fields =
        noFields()
                .that().arePublic()
                .and().areStatic()
                .and().areNotFinal()
                .should().exist();
```

Классы с mutable static singleton state дополнительно проверяются review/SpotBugs. Константы должны быть `private static final` или `public static final` только для stable API constants.

### 4.4 Deprecated API должен иметь замену

ArchUnit может проверить наличие Javadoc не идеально: Javadoc обычно недоступен в bytecode importer. Поэтому используем convention rule для класса и отдельный Javadoc/Checkstyle check.

```java
@ArchTest
static final ArchRule deprecated_types_must_be_in_legacy_package =
        classes()
                .that().areAnnotatedWith(Deprecated.class)
                .should().resideInAnyPackage("com.rimworldcraft..legacy..", "com.rimworldcraft..compat..");
```

В CI дополнительно required:

```text
@Deprecated(forRemoval = false)
Javadoc: @deprecated Use <replacement> instead. Removal target: <version>.
```

## 5. Правила Core и bounded contexts

### 5.1 Core package не зависит от Infrastructure/Client

```java
@ArchTest
static final ArchRule core_must_not_depend_on_outer_layers =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.rimworldcraft.infrastructure..",
                        "com.rimworldcraft.client..");
```

### 5.2 Прямые зависимости между bounded contexts запрещены

Более надёжная практика — явно перечислить запрещённые domain packages:

```java
@ArchTest
static final ArchRule colony_must_not_depend_on_sibling_domains =
        noClasses().that().resideInAnyPackage("com.rimworldcraft.core.colony..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.rimworldcraft.core.npc.domain..",
                        "com.rimworldcraft.core.story.domain..",
                        "com.rimworldcraft.core.goal.domain..",
                        "com.rimworldcraft.core.building.domain..");

@ArchTest
static final ArchRule npc_must_not_depend_on_sibling_domains =
        noClasses().that().resideInAnyPackage("com.rimworldcraft.core.npc..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.rimworldcraft.core.colony.domain..",
                        "com.rimworldcraft.core.story.domain..",
                        "com.rimworldcraft.core.goal.domain..",
                        "com.rimworldcraft.core.building.domain..");
```

Аналогичные rules добавляются для Story, Goal и Building. Допустимы `core.shared`, public contracts и ports.

### 5.3 Events не зависят от repositories

```java
@ArchTest
static final ArchRule events_must_not_depend_on_repositories =
        noClasses()
                .that().resideInAnyPackage(
                        "com.rimworldcraft.core..event..",
                        "com.rimworldcraft.core.events..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.rimworldcraft.core..repository..");
```

Event — immutable fact, а не handler и не persistence service.

### 5.4 Events не зависят от adapters

```java
@ArchTest
static final ArchRule events_must_not_depend_on_adapters =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..event..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.rimworldcraft.infrastructure..", "com.rimworldcraft.client..");
```

### 5.5 Aggregates находятся в правильных packages

```java
@ArchTest
static final ArchRule aggregates_must_reside_in_aggregate_package =
        classes()
                .that().haveSimpleNameEndingWith("Aggregate")
                .or().haveSimpleNameIn("Colony", "Citizen", "Storyteller",
                        "BuildOrder", "CitizenAI")
                .should().resideInAnyPackage("com.rimworldcraft.core.*.aggregate..");
```

Если aggregate root называется просто `Citizen`, конкретные roots лучше перечислить явно, чтобы rule не ошибочно применялся к DTO.

### 5.6 Aggregate не вызывает Repository

```java
@ArchTest
static final ArchRule aggregates_must_not_depend_on_repositories =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..aggregate..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.rimworldcraft.core..repository..");
```

Application service загружает aggregate, вызывает behavior и сохраняет его.

### 5.7 Aggregate не имеет public setters

ArchUnit-style field/method rule:

```java
@ArchTest
static final ArchRule aggregates_must_not_have_public_setters =
        noMethods()
                .that().arePublic()
                .and().haveNameMatching("set[A-Z].*")
                .and().areDeclaredInClassesThat()
                .resideInAnyPackage("com.rimworldcraft.core..aggregate..")
                .should().exist();
```

Если framework требует setter для hydration, он должен быть package-private mapper API или documented exception, а не public domain API.

### 5.8 Value objects immutable

```java
@ArchTest
static final ArchRule value_objects_must_be_final =
        classes()
                .that().resideInAnyPackage("com.rimworldcraft.core..valueobject..")
                .should().beFinal();

@ArchTest
static final ArchRule value_objects_must_not_have_setters =
        noMethods()
                .that().areDeclaredInClassesThat()
                .resideInAnyPackage("com.rimworldcraft.core..valueobject..")
                .and().haveNameMatching("set[A-Z].*")
                .should().exist();
```

`equals/hashCode` надёжнее проверять compile-time/EqualsVerifier или unit tests, потому что ArchUnit не является хорошим инструментом для доказательства value semantics. CI requirement:

```text
Every value object has equals/hashCode test or is a record with explicit value semantics.
```

### 5.9 Domain events наследуются от `DomainEvent`

```java
@ArchTest
static final ArchRule domain_events_must_extend_base_event =
        classes()
                .that().resideInAnyPackage(
                        "com.rimworldcraft.core.colony.event..",
                        "com.rimworldcraft.core.npc.event..",
                        "com.rimworldcraft.core.story.event..",
                        "com.rimworldcraft.core.goal.event..",
                        "com.rimworldcraft.core.building.event..")
                .should().beAssignableTo(DomainEvent.class);
```

### 5.10 Контексты не образуют циклы

```java
@ArchTest
static final ArchRule bounded_contexts_must_be_free_of_cycles =
        slices()
                .matching("com.rimworldcraft.core.(*)..")
                .should().beFreeOfCycles();
```

Если shared/contracts создают ложный цикл, вынести их в `core.shared`/`core.contracts`, а не подавлять rule без объяснения.

## 6. Порты и адаптеры

### 6.1 Ports находятся в Core

```java
@ArchTest
static final ArchRule ports_must_be_in_core =
        classes()
                .that().haveSimpleNameEndingWith("Port")
                .should().resideInAnyPackage("com.rimworldcraft.core.ports..");
```

### 6.2 Ports — interfaces

```java
@ArchTest
static final ArchRule ports_must_be_interfaces =
        classes()
                .that().resideInAnyPackage("com.rimworldcraft.core.ports..")
                .should().beInterfaces();
```

### 6.3 Ports не зависят от Infrastructure/Minecraft

```java
@ArchTest
static final ArchRule ports_must_not_depend_on_implementations =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core.ports..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.rimworldcraft.infrastructure..",
                        "net.minecraft..",
                        "net.minecraftforge..",
                        "net.fabricmc..");
```

Ports могут использовать Core domain types, shared IDs и DTO.

### 6.4 Adapters находятся в Infrastructure

```java
@ArchTest
static final ArchRule adapters_must_reside_in_infrastructure =
        classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAnyPackage("com.rimworldcraft.infrastructure.adapter..");
```

Для renderer используется отдельная client rule, потому что он не является port adapter в строгом смысле.

### 6.5 Driven adapters реализуют driven ports

```java
@ArchTest
static final ArchRule driven_adapters_must_implement_ports =
        classes()
                .that().resideInAnyPackage("com.rimworldcraft.infrastructure.adapter.driven..")
                .and().haveSimpleNameEndingWith("Adapter")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.rimworldcraft.core.ports.driven..");
```

Для гарантии `implements`, а не только import, использовать custom condition:

```java
ArchCondition<JavaClass> implementDrivenPort = new ArchCondition<>(
        "implement at least one driven port") {
    @Override
    public void check(JavaClass item, ConditionEvents events) {
        boolean implementsPort = item.getAllRawInterfaces().stream()
                .anyMatch(type -> type.getPackageName()
                        .startsWith("com.rimworldcraft.core.ports.driven"));
        if (!implementsPort) {
            events.add(SimpleConditionEvent.violated(
                    item, item.getName() + " does not implement a driven port"));
        }
    }
};
```

### 6.6 Driving adapters не используют driven ports напрямую

```java
@ArchTest
static final ArchRule driving_adapters_use_only_driving_ports =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.infrastructure.adapter.driving..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.rimworldcraft.core.ports.driven..",
                        "com.rimworldcraft.infrastructure.adapter.driven..");
```

Они вызывают application/driving ports; composition root собирает dependencies.

### 6.7 Core не импортирует adapters

```java
@ArchTest
static final ArchRule core_must_not_import_adapters =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.rimworldcraft.infrastructure.adapter..");
```

## 7. Repositories и factories

### 7.1 Repositories — interfaces в Core

```java
@ArchTest
static final ArchRule repositories_must_be_core_interfaces =
        classes()
                .that().resideInAnyPackage("com.rimworldcraft.core..repository..")
                .should().beInterfaces();
```

### 7.2 Repository contracts не зависят от storage

```java
@ArchTest
static final ArchRule repository_contracts_must_not_depend_on_storage =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..repository..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.rimworldcraft.infrastructure..",
                        "net.minecraft.nbt..",
                        "java.sql..",
                        "java.io..");
```

### 7.3 Repository implementations в Infrastructure

```java
@ArchTest
static final ArchRule repository_implementations_must_be_in_infrastructure =
        classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areNotInterfaces()
                .should().resideInAnyPackage("com.rimworldcraft.infrastructure.repository..");
```

Если implementation называется `NbtCitizenRepositoryAdapter`, правило меняется на suffix `RepositoryAdapter`.

### 7.4 Factories находятся в Core factory packages

```java
@ArchTest
static final ArchRule factories_must_be_in_core =
        classes()
                .that().haveSimpleNameEndingWith("Factory")
                .and().resideInAnyPackage("com.rimworldcraft.core..")
                .should().resideInAnyPackage("com.rimworldcraft.core.*.factory..");
```

Infrastructure factories, необходимые только для Minecraft registration, должны иметь иной suffix/package и не маскироваться под domain factory.

### 7.5 Aggregate factory не зависит от repository

```java
@ArchTest
static final ArchRule factories_must_not_persist =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..factory..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.rimworldcraft.core..repository..");
```

Factory создаёт; application service сохраняет.

## 8. Rules для логирования и исключений

### 8.1 Core не использует concrete logging framework без port policy

Если логирование домена запрещено или абстрагировано:

```java
@ArchTest
static final ArchRule core_must_not_depend_on_logging_implementation =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.apache.logging.log4j..",
                        "org.slf4j..",
                        "ch.qos.logback..");
```

Infrastructure может использовать logger. Если проект сознательно разрешает SLF4J facade в Core, rule изменяется на запрет concrete backend, а не facade.

### 8.2 Domain exceptions — наследники `DomainException`

```java
@ArchTest
static final ArchRule domain_exceptions_must_extend_domain_exception =
        classes()
                .that().resideInAnyPackage("com.rimworldcraft.core..exception..")
                .and().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(DomainException.class);
```

### 8.3 Storage exceptions не выходят в Core

```java
@ArchTest
static final ArchRule core_must_not_expose_storage_exceptions =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "java.sql..",
                        "java.nio.file..",
                        "java.io..",
                        "net.minecraft.nbt..");
```

## 9. Rules для конфигураций

### 9.1 Config loaders в Infrastructure

```java
@ArchTest
static final ArchRule config_loaders_must_be_in_infrastructure =
        classes()
                .that().haveSimpleNameEndingWith("ConfigLoader")
                .or().haveSimpleNameEndingWith("JsonParser")
                .should().resideInAnyPackage("com.rimworldcraft.infrastructure.config..");
```

### 9.2 Domain не читает файлы

```java
@ArchTest
static final ArchRule core_must_not_read_files_directly =
        noClasses()
                .that().resideInAnyPackage("com.rimworldcraft.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("java.nio.file..", "java.io..", "com.fasterxml.jackson..");
```

JSON parser implementation остаётся в Infrastructure; Core получает `ConfigSnapshot` через port.

Hardcoded path `config/rwc/` лучше проверять Checkstyle/custom static check. ArchUnit может запретить direct `Path` dependency, но не string literal reliably.

## 10. Rules для тестов

Тестовая архитектура должна соответствовать существующему layout:

```text
src/test/java/com/rimworldcraft/core/..
src/test/java/com/rimworldcraft/integration/..
src/test/java/com/rimworldcraft/acceptance/..
```

### 10.1 Core tests не зависят от Minecraft

```java
@AnalyzeClasses(packages = "com.rimworldcraft.core")
public class CoreTestArchitecture {
    @ArchTest
    static final ArchRule core_tests_must_not_depend_on_minecraft =
            noClasses()
                    .that().resideInAnyPackage("com.rimworldcraft.core..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("net.minecraft..", "net.minecraftforge..", "net.fabricmc..");
}
```

Если importer не включает test classes, это правило размещается в отдельном test-output importer.

### 10.2 Integration tests могут использовать Minecraft

Не следует запрещать их dependencies; вместо этого закрепить location:

```java
@ArchTest
static final ArchRule integration_tests_must_be_in_integration_package =
        classes()
                .that().haveSimpleNameEndingWith("IntegrationTest")
                .should().resideInAnyPackage("com.rimworldcraft.integration..");
```

### 10.3 Tags и `Thread.sleep`

`@Tag` и вызов `Thread.sleep` лучше проверяются custom ArchUnit condition/Checkstyle. Практический policy:

```text
UnitTest: @Tag("unit")
IntegrationTest: @Tag("integration")
AcceptanceTest: @Tag("acceptance")
PerformanceTest: @Tag("performance")
StressTest: @Tag("stress")
```

`Thread.sleep()` запрещён в tests; использовать deterministic executor, `Awaitility` или loader test clock. Static analysis rule может искать method call `java.lang.Thread.sleep`.

## 11. Coverage и public methods

ArchUnit не должен делать вид, что способен доказать, что каждый public aggregate method имеет meaningful test. Надёжная комбинация:

```text
ArchUnit: aggregate находится в правильном package и не имеет public setters
JaCoCo: aggregate >= configured branch/line threshold
JUnit API tests: каждый public behavior method invoked
Mutation testing: critical invariants survive mutations
```

Пример gate:

```text
Colony/Citizen: >=85%
Storyteller/Building/Goal AI: >=80%
Core overall: >=80%
Infrastructure overall: >=70%
```

## 12. Custom ArchUnit conditions

### 12.1 Проверка suffix и package

```java
static ArchCondition<JavaClass> haveAdapterSuffixAndPortDependency() {
    return new ArchCondition<>("be adapters backed by a Core port") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            boolean suffix = item.getSimpleName().endsWith("Adapter");
            boolean portDependency = item.getDirectDependenciesFromSelf().stream()
                    .anyMatch(dep -> dep.getTargetClass().getPackageName()
                            .startsWith("com.rimworldcraft.core.ports"));
            if (!suffix || !portDependency) {
                events.add(SimpleConditionEvent.violated(
                        item, item.getName() + " is not a valid port adapter"));
            }
        }
    };
}
```

### 12.2 Проверка immutable fields

Для value objects можно добавить custom condition, проверяющий все declared fields final. Для records это обычно redundant, но useful для обычных classes:

```java
static final ArchCondition<JavaClass> have_only_final_fields =
        new ArchCondition<>("have only final fields") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getAllFields().stream()
                        .filter(field -> !field.getModifiers().contains(JavaModifier.FINAL))
                        .forEach(field -> events.add(SimpleConditionEvent.violated(
                                item, field.getFullName() + " is not final")));
            }
        };
```

## 13. Exclusions и legacy code

### 13.1 Принцип

Exclusion — временное исключение из правила, а не способ сделать нарушение нормой. Каждое исключение содержит:

- rule ID;
- конкретный class/package/dependency;
- причину;
- owner;
- tracking issue;
- дату добавления;
- expiry/review date;
- план удаления.

### 13.2 Локальное исключение

Предпочтительно структурно изолировать legacy package и ограничить rule:

```java
@ArchTest
static final ArchRule core_rule = noClasses()
        .that().resideOutsideOfPackages("com.rimworldcraft.legacy..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("net.minecraft..");
```

ArchUnit имеет `ignoreDependency`/`FreezingArchRule` и другие механизмы, но глобальное игнорирование dependency нежелательно: оно может скрыть новые нарушения.

### 13.3 Формат registry исключений

```text
ARCH-EX-004
Rule: core_must_not_depend_on_minecraft
Dependency: LegacyWorldImporter -> net.minecraft.nbt.CompoundTag
Owner: @team-persistence
Issue: RWC-241
Added: 2026-09-01
Review by: 2026-10-01
Plan: migrate importer to SaveDocument
```

CI job проверяет, что expiry не истёк. Истёкшее исключение делает pipeline красным.

### 13.4 Когда исключение недопустимо

Нельзя исключать:

- новый Core → Minecraft import;
- новый domain → adapter dependency;
- security/authority violation;
- missing idempotency для critical event;
- flaky test через permanent ignore;
- правило только потому, что исправление неудобно.

## 14. Интеграция с CI/CD

### 14.1 Gradle

Пример pattern:

```groovy
dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:<pinned-version>")
}

tasks.register("architectureTest", Test) {
    description = "Runs ArchUnit architectural rules"
    group = "verification"
    useJUnitPlatform {
        includeTags("architecture")
    }
}

check.dependsOn(tasks.named("architectureTest"))
```

Если architecture tests не используют tag, достаточно отдельного source set/task:

```groovy
tasks.register("architectureTest", Test) {
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    filter { includeTestsMatching("*ArchitectureTest") }
}
```

### 14.2 PR pipeline

```yaml
name: verify

on:
  pull_request:

jobs:
  architecture:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - run: ./gradlew architectureTest --no-daemon
```

В проекте можно использовать `./gradlew` вместо `gradlew`, если executable bit гарантирован; hosting/CI convention должен быть единым. Результат публикуется как JUnit/HTML artifact.

### 14.3 Уровни pipeline

| Pipeline | Rules |
|---|---|
| PR fast | Core isolation, package boundaries, naming, forbidden practices |
| Release candidate | полный ArchUnit suite + all contract/integration tests |
| Nightly | full suite, dependency drift, legacy exclusions expiry, module matrix |

### 14.4 Failure output

Ошибка должна содержать:

- rule ID/name;
- violating class;
- forbidden dependency/condition;
- package owner;
- link на document/rule;
- remediation hint.

Пример:

```text
Rule core_must_not_depend_on_minecraft was violated:
com.rimworldcraft.core.npc.domain.Citizen -> net.minecraft.core.BlockPos
Use GridPosition and map it in infrastructure adapter.
```

## 15. Связь с DoD

ArchUnit является обязательным пунктом [`definition-of-done-do-d.md`](definition-of-done-do-d.md):

- Core isolation passed;
- bounded-context boundaries passed;
- ports/adapters placement passed;
- repositories/factories conventions passed;
- forbidden console/global state practices passed;
- no unapproved exclusions;
- architecture report attached to PR.

Нарушение rule означает, что задача не Done, пока dependency не исправлена или не оформлено временное исключение.

## 16. Добавление нового правила

1. Сформулировать архитектурную проблему и нарушение, которое rule предотвращает.
2. Определить scope: весь проект, Core, контекст или adapter.
3. Написать rule на минимальном наборе packages.
4. Добавить intentional violating fixture/test, если возможно.
5. Запустить на текущем коде и классифицировать legacy violations.
6. Исправить нарушения или создать expiry-bound exclusions.
7. Обновить этот документ и соответствующий architecture/module document.
8. Добавить rule ID в CI report.
9. Удалять/ослаблять rule только через ADR или review техлида.

## 17. Адаптация к изменениям проекта

### Новый bounded context

- добавить package constants;
- добавить forbidden sibling dependencies;
- включить event/port/repository rules;
- добавить context slice cycle check;
- добавить contract test boundaries.

### Новый loader/API

Добавить platform package (`net.neoforged..`, новый Fabric namespace и т.п.) в central forbidden list. Core rule должен оставаться неизменным.

### Выделение Gradle-модулей

Когда contexts станут отдельными modules, ArchUnit дополнить Gradle dependency checks. Package rules остаются полезными как defense in depth.

### Обновление ArchUnit

- зафиксировать version;
- прочитать migration notes;
- запускать старые и новые rules в переходный период;
- не использовать API, которого нет в pinned version;
- проверить false positives на records, sealed classes и generated code.

## 18. Связи с другими документами

- [`system-overview.md`](system-overview.md) — общая архитектура и контейнеры.
- [`bounded-contexts.md`](bounded-contexts.md) — границы контекстов и ownership.
- [`hexagonal-architecture.md`](hexagonal-architecture.md) — ports/adapters и dependency direction.
- [`codestyle-and-solidd.md`](codestyle-and-solidd.md) — style, SOLID и forbidden practices.
- [`ddd-tactical-patterns.md`](ddd-tactical-patterns.md) — aggregates, entities, value objects и repositories.
- [`definition-of-done-do-d.md`](definition-of-done-do-d.md) — обязательный DoD и CI evidence.
- [`testing-strategy.md`](testing-strategy.md) — unit/integration/contract testing.
- `drift-detection-pipeline.md` — расширенный CI/CD drift control.
- [`data-interfaces.md`](data-interfaces.md) — repository/factory/specification contracts.
- [`event-system-api.md`](event-system-api.md) — event inheritance, handlers и bus boundaries.
- [`save-serialization.md`](save-serialization.md) — persistence adapters и NBT isolation.

## 19. Практический checklist разработчика

Перед PR:

- [ ] Новый Core code не импортирует Minecraft/Forge/Fabric/Baritone.
- [ ] Код находится в правильном bounded context/package.
- [ ] Нет прямого sibling domain dependency.
- [ ] Aggregate не знает repository/adapter.
- [ ] Events наследуются от `DomainEvent` и не знают repositories.
- [ ] Ports находятся в Core и являются interfaces.
- [ ] Adapters находятся в Infrastructure и реализуют ports.
- [ ] Repository contracts не знают NBT/SQL/IO.
- [ ] Value objects immutable.
- [ ] Нет public mutable static state.
- [ ] Нет `System.out`, `System.err`, `printStackTrace`.
- [ ] Исключения — domain exceptions или корректно переведённые adapter errors.
- [ ] Test classes находятся в правильных packages и имеют tags.
- [ ] Нет `Thread.sleep()` в tests.
- [ ] Нет новых exclusions без issue/owner/expiry.
- [ ] `architectureTest` проходит локально и в CI.

## 20. Итог

ArchUnit rules RimWorldCraft — executable specification архитектуры. Они защищают независимость Core, bounded-context boundaries, DDD ownership, Ports and Adapters и coding conventions. Правила должны быть достаточно строгими, чтобы ловить drift сразу, но достаточно точными, чтобы не превращаться в бессмысленный список исключений.

Надёжный процесс — это не только один `noClasses()` rule, а сочетание package rules, custom conditions, coverage/static analysis, contract tests и expiry-bound exclusions. При изменении архитектуры сначала обновляются решения и документы, затем executable rules и только после этого production code.
