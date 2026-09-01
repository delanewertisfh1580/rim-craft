package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.events.*;
import com.rimworldcraft.core.api.types.*;
import java.time.Instant;
import java.util.*;
/** Build order aggregate root. */
public final class BuildOrder {
    private final UUID id; private final UUID colonyId; private final String blueprintId; private final Position targetPosition; private final int priority; private final Map<ResourceType, Integer> required; private final Map<ResourceType, Integer> consumed = new EnumMap<>(ResourceType.class); private final List<UUID> assigned = new ArrayList<>(); private BuildOrderStatus status = BuildOrderStatus.PENDING; private int progress; private final Instant createdAt = Instant.now();
    /** Creates a pending build order. */ public BuildOrder(UUID id, UUID colonyId, String blueprintId, Position targetPosition, int priority, Map<ResourceType, Integer> required) { this.id=Objects.requireNonNull(id); this.colonyId=Objects.requireNonNull(colonyId); this.blueprintId=Objects.requireNonNull(blueprintId); this.targetPosition=Objects.requireNonNull(targetPosition); if(priority<0) throw new IllegalArgumentException("priority"); this.priority=priority; this.required=new EnumMap<>(required); }
    /** Returns order identity. */ public UUID getId(){return id;} /** Returns status. */ public BuildOrderStatus getStatus(){return status;} /** Returns progress percentage. */ public int getProgress(){return progress;} /** Returns assigned citizen IDs. */ public List<UUID> getAssignedCitizenIds(){return List.copyOf(assigned);}
    /** Assigns a citizen. */ public List<DomainEvent> assignCitizen(UUID citizenId){ if(status!=BuildOrderStatus.PENDING&&status!=BuildOrderStatus.IN_PROGRESS) throw new IllegalStateException("Order is closed"); if(!assigned.contains(citizenId)) assigned.add(Objects.requireNonNull(citizenId)); status=BuildOrderStatus.IN_PROGRESS; return List.of(new BuildOrderAssignedEvent(id)); }
    /** Advances progress. */ public List<DomainEvent> updateProgress(int delta){ if(delta<0||status==BuildOrderStatus.COMPLETED||status==BuildOrderStatus.CANCELLED||status==BuildOrderStatus.FAILED) throw new IllegalArgumentException("Invalid progress update"); progress=Math.min(100,progress+delta); return List.of(new BuildOrderProgressedEvent(id)); }
    /** Consumes required resources. */ public void consumeResource(ResourceType type,int amount){ if(amount<=0||amount>required.getOrDefault(type,0)-consumed.getOrDefault(type,0)) throw new IllegalArgumentException("Invalid resource consumption"); consumed.merge(type,amount,Integer::sum); }
    /** Completes the order. */ public List<DomainEvent> complete(){ if(progress<100) throw new IllegalStateException("Order is not complete"); status=BuildOrderStatus.COMPLETED; return List.of(new BuildOrderCompletedEvent(id)); }
    /** Cancels the order. */ public List<DomainEvent> cancel(String reason){ if(status==BuildOrderStatus.COMPLETED) throw new IllegalStateException("Completed order cannot be cancelled"); status=BuildOrderStatus.CANCELLED; return List.of(new BuildOrderCancelledEvent(id)); }
    /** Fails the order. */ public List<DomainEvent> fail(String reason){ status=BuildOrderStatus.FAILED; return List.of(new BuildOrderFailedEvent(id)); }
}
