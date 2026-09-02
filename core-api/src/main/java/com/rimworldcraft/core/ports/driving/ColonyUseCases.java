package com.rimworldcraft.core.ports.driving;

import com.rimworldcraft.core.api.types.CitizenId;
import com.rimworldcraft.core.api.types.ColonyId;
import com.rimworldcraft.core.api.types.CommandId;
import com.rimworldcraft.core.api.types.PlayerId;
import com.rimworldcraft.core.api.types.ResourceType;
import com.rimworldcraft.core.api.types.WorldId;
import com.rimworldcraft.core.shared.ColonyName;
import com.rimworldcraft.core.shared.GridPosition;
import java.util.Objects;

/** Colony application use-case contracts. */
public final class ColonyUseCases {
    private ColonyUseCases() { }

    /** Creates a colony. */
    public interface CreateColonyUseCase { ColonyId create(CreateColonyCommand command); }
    /** Renames a colony. */
    public interface RenameColonyUseCase { void rename(RenameColonyCommand command); }
    /** Adds a citizen membership. */
    public interface AddCitizenUseCase { void add(AddCitizenCommand command); }
    /** Removes a citizen membership idempotently. */
    public interface RemoveCitizenUseCase { void remove(RemoveCitizenCommand command); }
    /** Assigns work to an existing member. */
    public interface AssignWorkUseCase { void assign(AssignWorkCommand command); }
    /** Reserves resources atomically. */
    public interface ReserveResourceUseCase { void reserve(ReserveResourceCommand command); }
    /** Applies a production result. */
    public interface ApplyProductionResultUseCase { void apply(ProductionResultCommand command); }
    /** Destroys a colony. */
    public interface DestroyColonyUseCase { void destroy(DestroyColonyCommand command); }

    /** Immutable colony creation command. */
    public record CreateColonyCommand(CommandId commandId, PlayerId playerId, WorldId worldId, ColonyName name, GridPosition position) {
        public CreateColonyCommand { require(commandId, playerId, worldId, name, position); }
    }
    /** Immutable rename command. */
    public record RenameColonyCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, ColonyName name) {
        public RenameColonyCommand { require(commandId, worldId, colonyId, name); }
    }
    /** Immutable membership addition command. */
    public record AddCitizenCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, CitizenId citizenId, PlayerId actorId) {
        public AddCitizenCommand { require(commandId, worldId, colonyId, citizenId, actorId); }
    }
    /** Immutable membership removal command. */
    public record RemoveCitizenCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, CitizenId citizenId, PlayerId actorId) {
        public RemoveCitizenCommand { require(commandId, worldId, colonyId, citizenId, actorId); }
    }
    /** Immutable work assignment command. */
    public record AssignWorkCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, CitizenId citizenId, String workType, PlayerId actorId) {
        public AssignWorkCommand { require(commandId, worldId, colonyId, citizenId, actorId); if (workType == null || workType.isBlank()) throw new IllegalArgumentException("workType must not be blank"); }
    }
    /** Immutable resource reservation command. */
    public record ReserveResourceCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, ResourceType resourceType, int amount, PlayerId actorId) {
        public ReserveResourceCommand { require(commandId, worldId, colonyId, resourceType, actorId); if (amount <= 0) throw new IllegalArgumentException("amount must be positive"); }
    }
    /** Immutable production result command. */
    public record ProductionResultCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, ResourceType resourceType, int amount, PlayerId actorId) {
        public ProductionResultCommand { require(commandId, worldId, colonyId, resourceType, actorId); if (amount <= 0) throw new IllegalArgumentException("amount must be positive"); }
    }
    /** Immutable destruction command. */
    public record DestroyColonyCommand(CommandId commandId, WorldId worldId, ColonyId colonyId, PlayerId actorId) {
        public DestroyColonyCommand { require(commandId, worldId, colonyId, actorId); }
    }

    private static void require(Object... values) { for (Object value : values) Objects.requireNonNull(value, "command value"); }
}
