package com.rimworldcraft.core.service;

import com.rimworldcraft.core.api.ports.IBlockWorldPort;
import com.rimworldcraft.core.api.ports.InventoryPort;
import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.npc.Citizen;
import com.rimworldcraft.core.building.*;
import java.util.*;

/** Legacy service declaration facade; new services should use one public type per file. */
final class ColonyValueCalculator { long calculate(Colony colony, IBlockWorldPort blockPort, InventoryPort inventoryPort) { Objects.requireNonNull(colony); Objects.requireNonNull(blockPort); Objects.requireNonNull(inventoryPort); return colony.recalculateValue(); } }
final class NeedDecayService { List<com.rimworldcraft.core.api.events.DomainEvent> updateNeeds(Citizen citizen, long ticks) { return citizen.updateNeeds(ticks); } }
final class MoodCalculationService { Mood recalculateMood(Citizen citizen) { return Objects.requireNonNull(citizen).getMood(); } }
final class ResourceManager { boolean reserveResources(Colony colony, Map<ResourceType,Integer> required) { for (var entry : required.entrySet()) { if (entry.getValue() == null || entry.getValue() < 0 || colony.getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) return false; } required.forEach((type, amount) -> { if (amount > 0) colony.removeResource(type, amount); }); return true; } void consumeResources(Colony colony, Map<ResourceType,Integer> resources) { resources.forEach(colony::removeResource); } void releaseResources(Colony colony, Map<ResourceType,Integer> resources) { resources.forEach(colony::addResource); } }
final class ProgressTracker { void updateProgress(BuildOrder order, int delta) { order.updateProgress(delta); } }
final class CollisionDetector { boolean checkCollision(Colony colony, Blueprint blueprint, Position pos) { return colony.getZones().stream().anyMatch(zone -> zone.contains(pos)); } }
