package com.rimworldcraft.core.repository;

import com.rimworldcraft.core.api.types.*;
import com.rimworldcraft.core.building.*;
import com.rimworldcraft.core.colony.Colony;
import com.rimworldcraft.core.npc.Citizen;
import com.rimworldcraft.core.story.*;
import java.util.*;

/**
 * Legacy repository declaration facade. New repository contracts should be moved
 * one interface per file under the bounded context's {@code port.out} package.
 */
interface IColonyRepository { Optional<Colony> findById(UUID id); void save(Colony colony); void delete(UUID id); List<Colony> findAllActive(); }
interface ICitizenRepository { Optional<Citizen> findById(UUID id); List<Citizen> findAllByColonyId(UUID colonyId); void save(Citizen citizen); void delete(UUID id); List<Citizen> findBySkillLevel(SkillType skill, int minLevel); }
interface IStorytellerRepository { Optional<Storyteller> findByColonyId(UUID colonyId); void save(Storyteller storyteller); void delete(UUID colonyId); }
interface IBuildOrderRepository { Optional<BuildOrder> findById(UUID id); List<BuildOrder> findAllByColonyId(UUID colonyId); List<BuildOrder> findByStatus(BuildOrderStatus status); void save(BuildOrder order); void delete(UUID id); }
interface IBlueprintRepository { Optional<Blueprint> findById(String id); List<Blueprint> findAll(); void save(Blueprint blueprint); }
interface IGhostBlockRepository { Optional<GhostBlock> findByPosition(Position pos); List<GhostBlock> findByBuildOrderId(UUID orderId); void save(GhostBlock ghostBlock); void deleteByPosition(Position pos); }
interface IIncidentRepository { List<Incident> findActiveByColonyId(UUID colonyId); List<Incident> findResolvedByColonyId(UUID colonyId); void save(Incident incident); }
