package com.rimworldcraft.core.service;

import com.rimworldcraft.core.api.ports.IBlockWorldPort;
import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.npc.Citizen;
import com.rimworldcraft.core.building.*;
import java.util.*;

/** Calculates colony value using domain state and world access. */
public final class ColonyValueCalculator { /** Calculates colony value. */ public long calculate(Colony colony, IBlockWorldPort blockPort, Object inventoryPort) { Objects.requireNonNull(colony); Objects.requireNonNull(blockPort); return colony.recalculateValue(); } }
/** Updates citizen needs. */
public final class NeedDecayService { /** Updates needs and returns domain events. */ public List<com.rimworldcraft.core.api.events.DomainEvent> updateNeeds(Citizen citizen, long ticks) { return citizen.updateNeeds(ticks); } }
/** Calculates citizen mood. */
public final class MoodCalculationService { /** Returns the current mood baseline. */ public Mood recalculateMood(Citizen citizen) { return Objects.requireNonNull(citizen).getMood(); } }
/** Reserves and transfers colony resources. */
public final class ResourceManager { /** Reserves all requested resources atomically. */ public boolean reserveResources(Colony colony, Map<ResourceType,Integer> required) { for(var e:required.entrySet()) if(colony.getResources().getOrDefault(e.getKey(),0)<e.getValue()) return false; required.forEach(colony::removeResource); return true; } /** Consumes resources. */ public void consumeResources(Colony colony, Map<ResourceType,Integer> resources) { resources.forEach(colony::removeResource); } /** Releases resources. */ public void releaseResources(Colony colony, Map<ResourceType,Integer> resources) { resources.forEach(colony::addResource); } }
/** Applies build progress. */
public final class ProgressTracker { /** Updates an order. */ public void updateProgress(BuildOrder order,int delta){ order.updateProgress(delta); } }
/** Performs placement collision checks. */
public final class CollisionDetector { /** Returns whether an existing zone contains the position. */ public boolean checkCollision(Colony colony, Blueprint blueprint, Position pos){ return colony.getZones().stream().anyMatch(zone->zone.contains(pos)); } }
