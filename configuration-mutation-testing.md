# Configuration mutation testing

**Status: PARTIAL.** Active configuration code covers schema validation, immutable snapshots, diagnostics, semantic helpers and a non-destructive mutator. Full loader orchestration is not active.

## Pipeline

```text
fixture → parse → schema validate → semantic validate → snapshot → context behavior
```

Mutations must operate on a copy or in-memory tree and never modify canonical fixtures.

## Mutation classes

- required field removed;
- wrong type;
- negative/out-of-range number;
- duplicate or unknown ID;
- missing cross-reference;
- empty collection;
- boundary/overflow value;
- old/future schema version;
- corrupt/missing file;
- valid value changed with expected behavior delta.

Every mutation has one explicit expectation: `ACCEPTED`, `REJECTED`, `CLAMPED`, `FALLBACK`, `IGNORED` or `FATAL`. Do not leave reject-versus-clamp ambiguous in production policy.

## Active evidence

- Validator: `infrastructure-common/.../JsonSchemaValidator.java`.
- Snapshot/publication: `ConfigSnapshot`, `AtomicConfigPublisher`.
- Diagnostics: `ConfigDiagnostic`.
- Mutator: `ConfigMutator`.
- Tests: `ConfigurationSmokeTest`, `ConfigValidatorTest`.
- Gradle tasks: `configTest`, `configSchemaTest`, `configMutationTest`.

## Pending

Full directory loader, per-context schema/semantic registry, persistent last-known-good orchestration, packaged defaults, critical/optional classification and real mutation-test reporting.

## Agent checklist

- Add schema, fixture, parser/mapper and negative tests together.
- Assert final status, effective snapshot and diagnostic path.
- Test isolation from source fixtures.
- Test cross-file references and future-version rejection.
- Update [`data-dictionaries.md`](data-dictionaries.md) and the owning module document.

## Verification

```bash
./gradlew :infrastructure-common:test --no-daemon
./gradlew :infrastructure-common:configTest :infrastructure-common:configSchemaTest :infrastructure-common:configMutationTest --no-daemon
```
