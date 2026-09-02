package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.ports.driven.*;
import com.rimworldcraft.core.ports.driving.BuildingUseCases;
import java.util.*;

public final class BuildingApplicationService implements BuildingUseCases {
    private final BuildOrderRepository orders; private final ResourceReservationPort resources; private final PlacementValidator placement; private final WorldMutationIntentPort world; private final Map<String,Blueprint> blueprints;
    public BuildingApplicationService(BuildOrderRepository orders,ResourceReservationPort resources,PlacementValidator placement,WorldMutationIntentPort world,Map<String,Blueprint> blueprints){this.orders=Objects.requireNonNull(orders);this.resources=Objects.requireNonNull(resources);this.placement=Objects.requireNonNull(placement);this.world=Objects.requireNonNull(world);this.blueprints=Map.copyOf(blueprints);}
    public BuildOrder create(UUID colonyId,UUID orderId,String blueprintId,GridPosition target,int priority){Blueprint blueprint=Optional.ofNullable(blueprints.get(blueprintId)).orElseThrow(()->new IllegalArgumentException("unknown blueprint"));if(!placement.isWithinBounds(blueprint,target,256,256,256)||!placement.isCollisionFree(blueprint,target)||blueprint.blocks().values().stream().anyMatch(b->!placement.isValidBlockType(b)))throw new IllegalArgumentException("invalid placement");BuildOrder order=new BuildOrder(orderId,colonyId,blueprint,target,priority);orders.save(order);world.submitPlacement(order);return order;}
    private BuildOrder load(UUID colonyId,UUID orderId){return orders.find(colonyId,orderId).orElseThrow(()->new IllegalArgumentException("order not found"));}
    public void reserve(UUID c,UUID o){BuildOrder order=load(c,o);if(!resources.reserve(o,order.blueprint().requiredResources()))throw new IllegalStateException("resources unavailable");order.reserve();orders.save(order);}
    public void assignCitizen(UUID c,UUID o,UUID citizen){BuildOrder order=load(c,o);order.assignCitizen(citizen);orders.save(order);}
    public void advance(UUID c,UUID o,int p){BuildOrder order=load(c,o);order.updateProgress(p);orders.save(order);}
    public void complete(UUID c,UUID o){BuildOrder order=load(c,o);order.complete();orders.save(order);world.submitPlacement(order);}
    public void cancel(UUID c,UUID o){BuildOrder order=load(c,o);order.cancel();resources.release(o);orders.save(order);world.submitRemoval(order);}
    public void fail(UUID c,UUID o){BuildOrder order=load(c,o);order.fail();resources.release(o);orders.save(order);world.submitRemoval(order);}
}
