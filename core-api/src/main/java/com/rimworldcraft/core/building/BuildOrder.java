package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.api.types.ResourceType;
import java.util.*;

public final class BuildOrder {
    private final UUID id, colonyId; private final Blueprint blueprint; private final GridPosition target; private final int priority;
    private final Map<ResourceType,Integer> consumed=new EnumMap<>(ResourceType.class); private final Set<UUID> assigned=new LinkedHashSet<>(); private final Set<UUID> appliedResults=new HashSet<>();
    private BuildOrderStatus status=BuildOrderStatus.PENDING; private int progress;
    public BuildOrder(UUID id,UUID colonyId,Blueprint blueprint,GridPosition target,int priority){this.id=Objects.requireNonNull(id);this.colonyId=Objects.requireNonNull(colonyId);this.blueprint=Objects.requireNonNull(blueprint);this.target=Objects.requireNonNull(target);if(priority<0)throw new IllegalArgumentException("priority");this.priority=priority;}
    public UUID id(){return id;} public UUID colonyId(){return colonyId;} public Blueprint blueprint(){return blueprint;} public GridPosition target(){return target;} public int priority(){return priority;} public BuildOrderStatus status(){return status;} public int progress(){return progress;} public Set<UUID> assignedCitizens(){return Set.copyOf(assigned);} public Map<ResourceType,Integer> consumed(){return Map.copyOf(consumed);}
    public void reserve(){if(status!=BuildOrderStatus.PENDING)throw new IllegalStateException("cannot reserve");status=BuildOrderStatus.RESERVED;}
    public void assignCitizen(UUID citizen){if(status!=BuildOrderStatus.RESERVED&&status!=BuildOrderStatus.IN_PROGRESS)throw new IllegalStateException("order not assignable");assigned.add(Objects.requireNonNull(citizen));status=BuildOrderStatus.IN_PROGRESS;}
    public void updateProgress(int delta){if(status==BuildOrderStatus.FAILED||status==BuildOrderStatus.CANCELLED||status==BuildOrderStatus.COMPLETED)throw new IllegalStateException("closed order");if(delta<0||progress+delta>100)throw new IllegalArgumentException("progress must remain 0..100");progress+=delta;}
    public boolean applyResourceResult(UUID resultId,ResourceType type,int amount){Objects.requireNonNull(resultId);if(!appliedResults.add(resultId))return false;if(amount<0||amount>blueprint.requiredResources().getOrDefault(type,0)-consumed.getOrDefault(type,0))throw new IllegalArgumentException("invalid resource result");consumed.merge(type,amount,Integer::sum);return true;}
    public void complete(){if(progress!=100)throw new IllegalStateException("progress must be 100");if(status==BuildOrderStatus.COMPLETED) return;if(status==BuildOrderStatus.FAILED||status==BuildOrderStatus.CANCELLED)throw new IllegalStateException("closed order");status=BuildOrderStatus.COMPLETED;}
    public void cancel(){if(status==BuildOrderStatus.COMPLETED)throw new IllegalStateException("completed order cannot be cancelled");if(status!=BuildOrderStatus.FAILED)status=BuildOrderStatus.CANCELLED;}
    public void fail(){if(status!=BuildOrderStatus.COMPLETED)status=BuildOrderStatus.FAILED;}
}
