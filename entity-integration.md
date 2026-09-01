# RimWorldCraft — Entity Integration

## 1. Назначение и границы

Этот документ описывает Minecraft-адаптер для NPC RimWorldCraft: регистрацию `EntityCitizen`, рендеринг, спавн, синхронизацию Core ↔ server entity ↔ clients, сетевые пакеты, NBT persistence и обработку игровых взаимодействий.

Интеграция является внешним слоем гексагональной архитектуры. Доменные правила находятся в `core.npc`, а Minecraft API разрешён только в `infrastructure.entity`, `infrastructure.adapter` и client/render packages.

Связанные контракты:

- [`hexagonal-architecture.md`](hexagonal-architecture.md) — `IEntitySpawnPort`;
- [`module-npc-core.md`](module-npc-core.md) — `Citizen`, needs, mood, skills и events;
- [`module-goal-ai.md`](module-goal-ai.md) — movement/action intents;
- [`data-interfaces.md`](data-interfaces.md) — `ICitizenRepository` и persistence contracts;
- `pathfinding-layer.md` — navigation implementation;
- `save-serialization.md` — общие NBT conventions.

### 1.1 Ответственность интеграции

Слой интеграции:

- регистрирует entity type и attributes в Forge/Fabric;
- связывает runtime entity UUID с доменным `CitizenId`;
- материализует `SpawnIntent` в Minecraft entity;
- проецирует authoritative Citizen state в synced entity data;
- отправляет snapshots клиентам через Minecraft networking;
- сохраняет минимальный identity/presentation state в NBT;
- преобразует player interaction в Core commands;
- возвращает Core action intents в Minecraft world.

### 1.2 Что не должен делать слой

- вычислять mood, needs, skills или priority;
- принимать окончательное решение о смерти, атаке или строительстве;
- читать/изменять `Citizen` aggregate напрямую из render thread;
- считать client prediction authoritative;
- использовать renderer для бизнес-логики;
- хранить единственную копию NPC state только в entity.

## 2. Архитектура потока данных

```text
                         SERVER
Minecraft tick -> EntityCitizen tick bridge
                       | observation / command
                       v
                NPC Core / Goal AI
                       | intents/events
                       v
          Entity adapters / network broadcaster
                       |
             Minecraft entity + clients

Client input -> packet -> server validation -> Core use case
Client render <- synced entity data/snapshot <- server
```

### 2.1 Source of truth

- Core repositories являются source of truth для доменного состояния.
- Server-side `EntityCitizen` — runtime projection и transport anchor.
- Client-side entity — визуальная/interpolation projection.
- При расхождении server entity и Core repository Core выигрывает; entity rehydrates из Core snapshot.

## 3. Структура пакетов

```text
com.rimworldcraft.infrastructure.entity/
  EntityCitizen.java
  CitizenEntityData.java
  CitizenEntityMapper.java
  CitizenEntityLifecycle.java

com.rimworldcraft.infrastructure.adapter/
  ForgeEntityAdapter.java
  FabricEntityAdapter.java
  EntitySpawnIntentMapper.java
  EntityInteractionAdapter.java

com.rimworldcraft.infrastructure.registry/
  ModEntityTypes.java
  ModEntityAttributes.java
  ForgeRegistryBootstrap.java
  FabricRegistryBootstrap.java

com.rimworldcraft.client.entity/
  CitizenRenderer.java
  CitizenModel.java
  CitizenRenderLayer.java
  CitizenClientDataHandler.java

com.rimworldcraft.infrastructure.network/
  CitizenStateS2CPacket.java
  CitizenActionC2SPacket.java
  CitizenSnapshotCodec.java
  CitizenNetworkHandler.java

com.rimworldcraft.core.npc/
  # Minecraft-independent Citizen and ports
```

## 4. `EntityCitizen`

### 4.1 Назначение

`EntityCitizen` — Minecraft runtime entity, связанный с `CitizenId`. Он отвечает за vanilla lifecycle, position interpolation, collision, animation state и transport hooks. Он не является доменным aggregate.

### 4.2 Поля и ownership

| Поле | Где хранится | Назначение |
|---|---|---|
| runtime UUID | Minecraft entity | runtime identity |
| `CitizenId` | entity synced/NBT data | связь с Core |
| `WorldId` | server context | scope validation |
| position/velocity | Minecraft entity | физическая projection |
| animation state | entity/client | визуальное состояние |
| authoritative needs/mood/skills | NPC Core repository | доменное состояние |
| task/goal | NPC Core/Goal AI | decision state |

### 4.3 Минимальный sketch

```java
public final class EntityCitizen extends PathfinderMob {
    private static final EntityDataAccessor<String> CITIZEN_ID =
            SynchedEntityData.defineId(EntityCitizen.class, EntityDataSerializers.STRING);

    public EntityCitizen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void bind(CitizenId citizenId) {
        entityData.set(CITIZEN_ID, citizenId.value().toString());
    }

    public Optional<CitizenId> citizenId() {
        String raw = entityData.get(CITIZEN_ID);
        return raw == null || raw.isBlank()
                ? Optional.empty()
                : Optional.of(new CitizenId(UUID.fromString(raw)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            lifecycle.onServerTick(this);
        }
    }
}
```

Конкретные superclass/mappings зависят от версии Minecraft и loader. Важно, что `CitizenId` — единственное доменное поле, которое entity должна знать напрямую.

## 5. Регистрация Forge/Fabric

### 5.1 Entity type

Регистрация выполняется на startup через loader-specific registry:

```java
public final class ModEntityTypes {
    public static final RegistryObject<EntityType<EntityCitizen>> CITIZEN =
            ENTITY_TYPES.register("citizen", () -> EntityType.Builder
                    .of(EntityCitizen::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .build("rimworldcraft:citizen"));
}
```

Для Fabric используется `Registry.register` с тем же logical ID и отдельным bootstrap. Общий Core не видит ни `RegistryObject`, ни Fabric `EntityType`.

### 5.2 Attributes и goals

Attributes должны регистрироваться до spawn. Minecraft AI goals (`PathfinderGoal`/equivalent) — adapter-level behavior, который получает intents от Goal AI; не помещать бизнес-priority в vanilla goal class.

### 5.3 Registry contract

- ID `rimworldcraft:citizen` стабилен после первого релиза.
- Изменение entity ID требует migration alias.
- Forge и Fabric используют одинаковый logical ID и data semantics.
- Registration выполняется один раз и проверяется integration test.

## 6. `CitizenRenderer`

### 6.1 Ответственность

Renderer отвечает только за:

- выбор модели/texture;
- interpolation position/rotation;
- animation state;
- render layers, equipment и effects;
- client-only visibility rules.

Renderer не вызывает `Citizen.die()`, `GoalAI.tick()`, repository, event bus или Minecraft world mutation.

```java
public final class CitizenRenderer extends MobRenderer<EntityCitizen, CitizenModel> {
    public CitizenRenderer(EntityRendererProvider.Context context) {
        super(context, new CitizenModel(context.bakeLayer(CitizenModel.LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCitizen entity) {
        return TextureResolver.forCitizen(entity.citizenId());
    }
}
```

### 6.2 Client data

Renderer может читать synced cosmetic fields (`role`, `moodBand`, `animation`) через client entity data. Точные `mood`/health values не должны передаваться клиенту, если UI permission/security policy этого не разрешает.

## 7. `ForgeEntityAdapter` и `IEntitySpawnPort`

### 7.1 Контракт

```java
public interface IEntitySpawnPort {
    SpawnResult spawn(SpawnIntent intent);
    DespawnResult despawn(EntityId entityId);
    Optional<EntitySnapshot> find(EntityId entityId);
    void applyIntent(EntityIntent intent);
}
```

### 7.2 Реализация

```java
public final class ForgeEntityAdapter implements IEntitySpawnPort {
    private final ServerContext server;
    private final CitizenEntityMapper mapper;

    @Override
    public SpawnResult spawn(SpawnIntent intent) {
        validateServerThread();
        ServerLevel level = server.level(intent.position().worldId());
        EntityCitizen entity = ModEntityTypes.CITIZEN.get()
                .create(level);
        if (entity == null) return SpawnResult.failure("ENTITY_CREATE_FAILED");

        entity.setPos(intent.position().x(), intent.position().y(), intent.position().z());
        entity.bind(new CitizenId(intent.requestedId().value()));
        level.addFreshEntity(entity);
        mapper.remember(intent.requestedId(), entity.getUUID());
        return SpawnResult.success(new EntityId(entity.getUUID()));
    }

    @Override
    public DespawnResult despawn(EntityId entityId) {
        validateServerThread();
        return mapper.findRuntime(entityId)
                .map(entity -> { entity.discard(); return DespawnResult.success(); })
                .orElse(DespawnResult.notFound());
    }
}
```

В реальном проекте mapping между `CitizenId` и runtime UUID должен быть устойчив к reload/reconnect и проверять `WorldId`. `EntityId` не обязан совпадать с `CitizenId`.

### 7.3 Ошибки adapter

Minecraft exceptions переводятся в `SpawnResult.failure`/`PortAccessException`. Adapter не публикует `NPCCreatedEvent`: creation принадлежит NPC Core; adapter только материализует уже принятое решение.

## 8. Синхронизация Core и Entity

### 8.1 Направления

```text
Core -> server entity: authoritative snapshot/projection
server entity -> Core: observations and validated player actions
server -> clients: synced entity data + explicit packets
client -> server: commands only
```

### 8.2 Периоды синхронизации

Рекомендуемая политика:

| Данные | Частота | Механизм |
|---|---:|---|
| position/rotation | каждый Minecraft tick через vanilla tracking | entity sync/interpolation |
| `CitizenId` | при spawn/load | synced data + NBT |
| goal/task summary | каждые 10–20 ticks или при change | S2C packet |
| mood band/need bands | при значимом изменении или 20–40 ticks | S2C packet |
| full citizen view | по запросу/permission | query packet |
| Core snapshot to entity | 10–20 ticks или after event | server projection service |
| AI plan debug | только debug mode | optional packet |

«Каждые N тиков» — configurable interval, но critical events (`NPCDeathEvent`, lethal damage, task cancellation) обрабатываются немедленно на server.

### 8.3 Reconciliation

`CitizenEntitySyncService`:

1. получает server tick;
2. читает `CitizenSummary` из repository/projection;
3. сравнивает revision с entity projection revision;
4. обновляет synced fields только при изменении;
5. отправляет packet tracking clients;
6. если entity потеряла binding — rebind или despawn, не создавая новый Citizen.

```java
public final class CitizenEntitySyncService {
    public void sync(EntityCitizen entity) {
        CitizenId id = entity.citizenId().orElseThrow();
        CitizenSummary summary = npcQueries.summary(id);
        if (summary.revision() <= entity.projectionRevision()) return;
        entity.applyProjection(summary.moodBand(), summary.role(), summary.currentTaskId());
        packets.broadcast(new CitizenStateS2CPacket(entity, summary));
    }
}
```

### 8.4 Revision и stale packets

Каждый projection/snapshot содержит monotonic `revision` или server tick. Клиент отбрасывает packet с revision меньше уже принятой. Runtime entity UUID и `CitizenId` проверяются на server; client не может прислать собственные mood/position facts как authoritative.

## 9. Сетевые пакеты

### 9.1 Категории

- `CitizenStateS2CPacket` — server → tracking clients, компактная projection;
- `CitizenSnapshotS2CPacket` — server → authorized player, расширенная UI view;
- `CitizenActionC2SPacket` — client → server, намерение игрока;
- `CitizenDebugS2CPacket` — optional operator/debug mode.

### 9.2 S2C packet

```java
public record CitizenStateS2CPacket(
        UUID runtimeEntityId,
        UUID citizenId,
        long revision,
        String role,
        String moodBand,
        Optional<UUID> currentTaskId
) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(runtimeEntityId);
        buffer.writeUUID(citizenId);
        buffer.writeVarLong(revision);
        buffer.writeUtf(role, 32);
        buffer.writeUtf(moodBand, 32);
        buffer.writeBoolean(currentTaskId.isPresent());
        currentTaskId.ifPresent(buffer::writeUUID);
    }
}
```

Не отправлять клиенту весь aggregate, private relationships или secrets. Размеры strings/collections ограничиваются до decode.

### 9.3 C2S packet validation

```java
public final class CitizenActionPacketHandler {
    public void handle(CitizenActionC2SPacket packet, ServerPlayer sender) {
        server.execute(() -> {
            CitizenId claimed = packet.citizenId();
            if (!authority.canControl(sender, claimed)) {
                sendError(sender, "NOT_AUTHORIZED");
                return;
            }
            if (!withinReach(sender, packet.target())) {
                sendError(sender, "TARGET_OUT_OF_RANGE");
                return;
            }
            core.handlePlayerAction(mapper.toCommand(sender, packet));
        });
    }
}
```

Каждый packet проверяет:

- sender identity из server connection, а не из payload;
- world/dimension;
- entity existence и ownership;
- distance/line of sight;
- enum/length/range;
- rate limit и replay `commandId`;
- server thread.

## 10. Взаимодействия игрока

### 10.1 ПКМ

```text
Player right-clicks EntityCitizen
  -> interaction adapter validates server authority
  -> creates PlayerActionCommand
  -> NPC/Goal/Colony Core use case
  -> event/result
  -> notification packet or entity projection
```

ПКМ может открыть UI, назначить разговор или взять под контроль NPC, но все permissions решаются Core/Player context.

### 10.2 Атака

Minecraft damage event adapter преобразует hit в `ApplyDamageCommand` NPC Core. Он не вызывает `Citizen.die()` напрямую. NPC Core применяет health policy и публикует `NPCDeathEvent`; adapter затем despawns entity after authoritative event.

### 10.3 Entity interaction event

Forge/Fabric event handler — thin adapter:

```java
public InteractionResult onInteract(Player player, EntityCitizen entity, Hand hand) {
    if (player.level().isClientSide()) return InteractionResult.PASS;
    PlayerActionCommand command = mapper.interact(player, entity, hand);
    CommandResult result = core.handle(command);
    return result.success() ? InteractionResult.CONSUME : InteractionResult.FAIL;
}
```

## 11. NBT persistence

### 11.1 Что сохраняет entity

Entity NBT содержит только runtime identity/projection data:

```text
citizenId
worldId
projectionRevision
role/display metadata
lastKnownCoreVersion
```

Полные needs, skills, traits, relationships, mood history и tasks сохраняются через `ICitizenRepository`/`ISaveLoadPort`.

### 11.2 Методы

```java
@Override
protected void addAdditionalSaveData(CompoundTag tag) {
    citizenId().ifPresent(id -> tag.putUUID("CitizenId", id.value()));
    tag.putLong("ProjectionRevision", projectionRevision());
}

@Override
protected void readAdditionalSaveData(CompoundTag tag) {
    if (tag.hasUUID("CitizenId")) {
        entityData.set(CITIZEN_ID, tag.getUUID("CitizenId").toString());
    }
    projectionRevision = tag.getLong("ProjectionRevision");
}
```

### 11.3 Rebind после загрузки

После world load:

1. entity читает `citizenId`;
2. adapter ищет Citizen snapshot;
3. если найден — rehydrates projection;
4. если отсутствует — entity помещается в quarantine/despawn policy и логируется;
5. новый Citizen автоматически не создаётся, чтобы не размножить NPC.

## 12. Регистрация renderer и network

### Forge

- client setup регистрирует `EntityRenderersEvent.RegisterRenderers`;
- common setup регистрирует packet channel/types;
- server lifecycle регистрирует tick and entity event adapters.

### Fabric

- client initializer регистрирует renderer через `EntityRendererRegistry`;
- common initializer регистрирует payload types/handlers;
- server lifecycle подключает tick/entity callbacks.

Общий packet DTO и Core command mapper желательно держать в `infrastructure.network.common`, а codec/buffer APIs — в loader-specific modules.

## 13. Тестирование

### 13.1 Unit

Без Minecraft runtime:

- `CitizenEntityMapper` correctly maps IDs;
- `SpawnIntent` rejects wrong world/invalid position;
- packet codec round-trip for DTO;
- packet bounds reject oversized values;
- revision reconciliation ignores stale snapshots;
- interaction mapper produces correct Core command;
- renderer model selection tests use plain data fixtures, without business logic.

### 13.2 Adapter contract tests

Один abstract suite запускается для Forge/Fabric:

```java
abstract class EntitySpawnAdapterContractTest {
    protected abstract IEntitySpawnPort adapter();

    @Test
    void spawnBindsCitizenId() {
        SpawnIntent intent = SpawnFixtures.citizen();
        SpawnResult result = adapter().spawn(intent);
        assertThat(result.success()).isTrue();
        assertThat(adapter().find(result.entityId()).orElseThrow().citizenId())
                .isEqualTo(intent.citizenId());
    }

    @Test
    void wrongWorldIsRejected() { /* ... */ }
    @Test
    void despawnIsIdempotent() { /* ... */ }
}
```

### 13.3 Integration

- entity type registered under stable ID;
- renderer registered on client;
- spawn/despawn in test world;
- Core snapshot survives NBT save/load;
- `citizenId` restored;
- S2C packet reaches tracking client;
- stale packet ignored;
- C2S packet cannot control another player’s citizen;
- right-click/attack calls Core only server-side;
- death event removes Colony membership through event chain;
- unload/shutdown closes handlers and avoids duplicate registration.

### 13.4 Acceptance Gherkin

```gherkin
Feature: Multiplayer citizen synchronization

  Scenario: Server synchronizes citizen state to tracking clients
    Given a server citizen with citizenId "c-123"
    And two clients are tracking the entity
    When the citizen mood band changes on the server
    Then one state snapshot with a newer revision is sent
    And both clients display the new mood band
    And a stale snapshot is ignored

  Scenario: Client cannot control another player's citizen
    Given player "alex" owns citizen "c-123"
    And player "sam" sends an action for citizen "c-123"
    When the server validates the packet
    Then the action is rejected with "NOT_AUTHORIZED"
    And no Core state is changed
```

## 14. DoD интеграции сущностей

- [ ] `EntityCitizen` создан и зарегистрирован в Forge/Fabric.
- [ ] `CitizenRenderer` создан и зарегистрирован.
- [ ] `ForgeEntityAdapter`/`FabricEntityAdapter` реализуют `IEntitySpawnPort` и протестированы.
- [ ] Синхронизация Core ↔ server entity ↔ clients работает через configurable N ticks и immediate critical events.
- [ ] Сетевые пакеты реализованы, декодируются с bounds checks и синхронизируют состояние с авторизованными клиентами.
- [ ] Сущности корректно сохраняются/загружаются из NBT и сохраняют `citizenId`.
- [ ] ПКМ и атака проходят server validation и вызывают соответствующие Core use cases.
- [ ] Написаны интеграционные тесты спавна, persistence, packets и базового поведения.
- [ ] ArchUnit rules не нарушены.
- [ ] Проверены stale packet, duplicate command, wrong world, unload и reconnect cases.
- [ ] Renderer не содержит бизнес-логики.

## 15. ArchUnit-правила интеграции

### 15.1 Infrastructure entity может зависеть от Minecraft, но ограниченно от Core

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.entity..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core..",
        "com.rimworldcraft.core.npc.domain..",
        "com.rimworldcraft.core.colony.domain.."
    );
```

Разрешены только Core ports, events, DTO и mappers:

```java
classes()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.adapter..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core.ports..",
        "com.rimworldcraft.core.events..",
        "com.rimworldcraft.core.contracts..",
        "net.minecraft..",
        "net.minecraftforge..",
        "net.fabricmc..",
        "java.."
    );
```

### 15.2 Adapter реализует `IEntitySpawnPort`

```java
classes()
    .that().haveSimpleNameEndingWith("EntityAdapter")
    .and().resideInAnyPackage("com.rimworldcraft.infrastructure.adapter..")
    .should().implement(IEntitySpawnPort.class);
```

### 15.3 Renderer не зависит от Core services/repositories

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.client..render..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core..service..",
        "com.rimworldcraft.core..repository..",
        "com.rimworldcraft.core..application.."
    );
```

Renderer может зависеть от entity presentation data, textures и Minecraft rendering API.

### 15.4 Запрет `System.out`

```java
noClasses()
    .that().resideInAnyPackage(
        "com.rimworldcraft.infrastructure.entity..",
        "com.rimworldcraft.infrastructure.adapter..",
        "com.rimworldcraft.client.."
    )
    .should().accessClassesThat()
    .belongToAnyOf(System.class);
```

### 15.5 C2S handlers не вызывают domain aggregates напрямую

```java
noClasses()
    .that().resideInAnyPackage("com.rimworldcraft.infrastructure.network..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "com.rimworldcraft.core.npc.domain..",
        "com.rimworldcraft.core.colony.domain.."
    );
```

Они вызывают driving ports/application commands.

## 16. Безопасность и производительность

- payload limits для всех strings, arrays и maps;
- rate limit C2S packets;
- server derives actor identity from connection;
- `CitizenId`, runtime UUID и WorldId cross-checked;
- client никогда не является source of truth;
- packets отправляются только tracking/authorized players;
- full citizen data не broadcast;
- projection sync batch-wise, critical state immediately;
- entity lookup не выполняется глобальным world scan на каждом tick;
- mapping cache очищается при unload;
- network handler не блокирует server tick на IO.

## 17. План расширения

### 17.1 Летающие NPC

Создать `EntityFlyingCitizen` и отдельный movement adapter, сохранив `CitizenId`, `IEntitySpawnPort` и packet semantics. Не добавлять flying logic в `Citizen` aggregate.

### 17.2 Транспорт

Транспорт должен быть отдельной runtime entity/aggregate projection. Citizen получает `MountIntent`/`MountedState` через Core contract, а entity integration управляет seat/vehicle mapping.

### 17.3 Новые модели и анимации

- renderer strategy выбирается по `presentationType`;
- textures/animations регистрируются client-only;
- server передаёт только compact animation state;
- animation state не меняет domain state;
- compatibility fallback используется при неизвестной модели.

### 17.4 Другие loaders

Добавляется новый adapter/registry/bootstrap module, который реализует те же Core ports и packet contracts. `EntityCitizen`, renderer и loader code могут иметь platform-specific variants, но Citizen Core не меняется.

### 17.5 Offline/reconnect

При reconnect клиент получает authoritative snapshot с revision. При entity unload/reload runtime UUID может измениться, поэтому `CitizenId` — основной ключ reconciliation.

## 18. Связи с другими документами

- [`hexagonal-architecture.md`](hexagonal-architecture.md) — `IEntitySpawnPort`, DTO, adapters и composition root.
- [`module-npc-core.md`](module-npc-core.md) — `Citizen`, NPC lifecycle, needs, mood, skills и `NPCDeathEvent`.
- [`module-goal-ai.md`](module-goal-ai.md) — `MOVE_TO`, `ATTACK`, action intents и replanning.
- `pathfinding-layer.md` — навигация entity через `IPathfinderPort`.
- `save-serialization.md` — NBT conventions, snapshots и migrations.
- [`data-interfaces.md`](data-interfaces.md) — `ICitizenRepository`, async/persistence contracts.
- [`event-system-api.md`](event-system-api.md) — delivery NPC/Colony/Goal AI events.
- [`bounded-contexts.md`](bounded-contexts.md) — ownership IDs и запрет прямого доступа к агрегатам.

## 19. Итог

`EntityCitizen` — runtime-проекция, а `Citizen` — доменный aggregate. Server Core принимает решения, entity adapter выполняет их в Minecraft, а clients получают ограниченные versioned projections. Такой контракт обеспечивает корректный multiplayer, безопасные packets, восстановление через `citizenId`, совместимость Forge/Fabric и сохранение полной независимости Core от Minecraft API.
