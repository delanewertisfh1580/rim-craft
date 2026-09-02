package com.rimworldcraft.core.colony;

import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.ports.driven.ColonyRepository;
import com.rimworldcraft.core.ports.driven.SettlementValidationPort;
import com.rimworldcraft.core.ports.driving.ColonyUseCases.*;
import com.rimworldcraft.core.shared.ColonyName;
import java.util.Objects;
import java.util.UUID;

/** Application service coordinating Colony use cases through ports. */
public final class ColonyApplicationService implements CreateColonyUseCase, RenameColonyUseCase,
        AddCitizenUseCase, RemoveCitizenUseCase, ReserveResourceUseCase,
        ApplyProductionResultUseCase, DestroyColonyUseCase {
    private final ColonyRepository repository;
    private final SettlementValidationPort settlement;

    /** Creates a service with explicit driven dependencies. */
    public ColonyApplicationService(ColonyRepository repository, SettlementValidationPort settlement) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.settlement = Objects.requireNonNull(settlement, "settlement");
    }

    /** Creates and persists a colony after settlement validation. */
    @Override public ColonyId create(CreateColonyCommand command) {
        Objects.requireNonNull(command, "command");
        if (!settlement.isValid(command.worldId(), command.position().toApiType())) throw new IllegalArgumentException("INVALID_SITE");
        Colony colony = new Colony(command.worldId(), new ColonyId(UUID.randomUUID()), command.name());
        repository.save(new ColonyRepository.ColonyRecord(colony.worldId(), colony.id(), colony.getName(), colony.isActive()));
        return colony.id();
    }
    /** Renames an existing colony. */
    @Override public void rename(RenameColonyCommand command) { colony(command.worldId(), command.colonyId()).rename(command.name()); }
    /** Adds a citizen to a colony. */
    @Override public void add(AddCitizenCommand command) { Colony colony = colony(command.worldId(), command.colonyId()); colony.addCitizen(command.citizenId()); save(colony); }
    /** Removes a citizen idempotently. */
    @Override public void remove(RemoveCitizenCommand command) { Colony colony = colony(command.worldId(), command.colonyId()); colony.removeCitizen(command.citizenId()); save(colony); }
    /** Reserves resources by atomically validating and removing the requested amount. */
    @Override public void reserve(ReserveResourceCommand command) { Colony colony = colony(command.worldId(), command.colonyId()); if (colony.getResources().getOrDefault(command.resourceType(), 0) < command.amount()) throw new IllegalArgumentException("INSUFFICIENT_RESOURCES"); colony.removeResource(command.resourceType(), command.amount()); save(colony); }
    /** Applies production to a colony. */
    @Override public void apply(ProductionResultCommand command) { Colony colony = colony(command.worldId(), command.colonyId()); colony.addResource(command.resourceType(), command.amount()); save(colony); }
    /** Deactivates a colony. */
    @Override public void destroy(DestroyColonyCommand command) { Colony colony = colony(command.worldId(), command.colonyId()); if (colony.isActive()) { colony.deactivate(); save(colony); } }
    private Colony colony(WorldId worldId, ColonyId colonyId) { ColonyRepository.ColonyRecord record = repository.findById(worldId, colonyId).orElseThrow(() -> new IllegalArgumentException("COLONY_NOT_FOUND")); return new Colony(record.worldId(), record.colonyId(), new ColonyName(record.name())); }
    private void save(Colony colony) { repository.save(new ColonyRepository.ColonyRecord(colony.worldId(), colony.id(), colony.getName(), colony.isActive())); }
}
