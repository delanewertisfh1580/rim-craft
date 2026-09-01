package com.rimworldcraft.core.repository;

import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.building.*;
import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.npc.Citizen;
import com.rimworldcraft.core.story.*;
import java.util.*;

/** Colony repository port. */
public interface IColonyRepository { /** Finds a colony. */ Optional<Colony> findById(UUID id); /** Saves a colony. */ void save(Colony colony); /** Deletes a colony. */ void delete(UUID id); /** Finds active colonies. */ List<Colony> findAllActive(); }
/** Citizen repository port. */
public interface ICitizenRepository { /** Finds a citizen. */ Optional<Citizen> findById(UUID id); /** Finds citizens in a colony. */ List<Citizen> findAllByColonyId(UUID colonyId); /** Saves a citizen. */ void save(Citizen citizen); /** Deletes a citizen. */ void delete(UUID id); /** Finds citizens meeting a skill threshold. */ List<Citizen> findBySkillLevel(SkillType skill, int minLevel); }
/** Storyteller repository port. */
public interface IStorytellerRepository { /** Finds storyteller by colony. */ Optional<Storyteller> findByColonyId(UUID colonyId); /** Saves storyteller. */ void save(Storyteller storyteller); /** Deletes storyteller. */ void delete(UUID colonyId); }
/** Build order repository port. */
public interface IBuildOrderRepository { /** Finds an order. */ Optional<BuildOrder> findById(UUID id); /** Finds colony orders. */ List<BuildOrder> findAllByColonyId(UUID colonyId); /** Finds orders by status. */ List<BuildOrder> findByStatus(BuildOrderStatus status); /** Saves an order. */ void save(BuildOrder order); /** Deletes an order. */ void delete(UUID id); }
/** Blueprint repository port. */
public interface IBlueprintRepository { /** Finds a blueprint. */ Optional<Blueprint> findById(String id); /** Finds all blueprints. */ List<Blueprint> findAll(); /** Saves a blueprint. */ void save(Blueprint blueprint); }
/** Ghost block repository port. */
public interface IGhostBlockRepository { /** Finds a block at a position. */ Optional<GhostBlock> findByPosition(Position pos); /** Finds order blocks. */ List<GhostBlock> findByBuildOrderId(UUID orderId); /** Saves a ghost block. */ void save(GhostBlock ghostBlock); /** Deletes a position block. */ void deleteByPosition(Position pos); }
/** Incident repository port. */
public interface IIncidentRepository { /** Finds active incidents. */ List<Incident> findActiveByColonyId(UUID colonyId); /** Finds resolved incidents. */ List<Incident> findResolvedByColonyId(UUID colonyId); /** Saves an incident. */ void save(Incident incident); }
