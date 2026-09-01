# RimWorldCraft — Stage 1: `core-api`

Java 17 API foundation for the RimWorldCraft Minecraft mod. This stage contains only platform-independent contracts: value objects, ports, domain events, event handlers, and domain exceptions.

## Build

Requires Gradle 8.x and JDK 17.

```bash
gradle :core-api:build
```

`core-api` has no Minecraft, Forge, Fabric, logging, JSON, or test-library dependencies. Future modules are intentionally not included until their implementation stages begin.

## Package layout

```text
com.rimworldcraft.core.api
├── types
├── ports
├── events
├── handlers
└── exceptions
```

All public contracts are documented with concise English Javadoc as required by the project coding standard.
