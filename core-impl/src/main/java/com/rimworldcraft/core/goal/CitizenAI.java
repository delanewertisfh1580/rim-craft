package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.api.types.*;
import java.util.*;
/** Citizen AI aggregate; see module-goal-ai.md. */
public final class CitizenAI {
    private final UUID citizenId; private Goal currentGoal; private Plan currentPlan; private final Queue<Action> actionQueue=new ArrayDeque<>();
    /** Creates AI state. */ public CitizenAI(UUID citizenId){this.citizenId=Objects.requireNonNull(citizenId);}
    /** Returns citizen ID. */ public UUID getCitizenId(){return citizenId;} /** Returns current plan. */ public Optional<Plan> getCurrentPlan(){return Optional.ofNullable(currentPlan);}
    /** Processes a world snapshot. */ public void tick(WorldState state){Objects.requireNonNull(state);}
    /** Builds a one-action plan. */ public void replan(Goal goal,WorldState state){Objects.requireNonNull(goal);Objects.requireNonNull(state); currentGoal=goal; currentPlan=new Plan(List.of(goal.targetAction()),goal.targetAction().cost(),PlanStatus.PENDING); actionQueue.clear(); actionQueue.add(goal.targetAction());}
    /** Returns and removes the next action. */ public Optional<Action> executeNextAction(){return Optional.ofNullable(actionQueue.poll());}
}
