package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.ports.driven.GoalActionIntentPort;
import com.rimworldcraft.core.ports.driven.GoalAiEventPort;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoalAiContextTest {
    private static final WorldId WORLD = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    private static final CitizenId CITIZEN = new CitizenId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
    private static final GridPosition START = new GridPosition(0, 64, 0);
    private static final GridPosition TARGET = new GridPosition(3, 64, 0);

    @Test
    void plannerHonoursMaximumDepthAndDeterministicEffects() {
        ActionDefinition move = action("move", ActionType.MOVE_TO, 1,
                Set.of(new StateFact("at_home", true)), Set.of(new StateFact("at_wall", true)));
        ActionDefinition place = action("place", ActionType.PLACE_BLOCK, 2,
                Set.of(new StateFact("at_wall", true)), Set.of(new StateFact("wall_built", true)));
        WorldState state = state(Set.of(new StateFact("at_home", true)), 0);
        Goal goal = new Goal(GoalType.BUILD, 75, "wall_built");

        Optional<Plan> plan = BasicGoalPolicies.planner(List.of(place, move), 2).plan(goal, state);

        assertTrue(plan.isPresent());
        assertEquals(List.of("move", "place"), plan.orElseThrow().actions().stream().map(ActionDefinition::id).toList());
        assertTrue(BasicGoalPolicies.planner(List.of(place, move), 1).plan(goal, state).isEmpty());
    }

    @Test
    void goalSelectionUsesConfiguredPriorityAndStableTieBreaking() {
        PriorityEvaluator evaluator = (goal, ignored) -> goal == GoalType.BUILD || goal == GoalType.WORK ? 50 : 0;

        Goal selected = BasicGoalPolicies.selector(evaluator).select(state(Set.of(), 0));

        assertEquals(GoalType.WORK, selected.type());
    }

    @Test
    void saveDocumentRoundTripRestoresPlanAndExecutionCursor() {
        Goal goal = new Goal(GoalType.BUILD, 75, "wall_built");
        ActionDefinition action = action("place", ActionType.PLACE_BLOCK, 2, Set.of(), Set.of(new StateFact("wall_built", true)));
        Plan plan = new Plan(UUID.fromString("00000000-0000-0000-0000-000000000012"), goal, List.of(action), 2, 4);
        CitizenAIState original = new CitizenAIState(CITIZEN, WORLD, goal, plan, AIStatus.ACTIVE, 1, 0, 5, 2, 5);

        GoalAiSnapshotMapper mapper = new GoalAiSnapshotMapper();
        CitizenAIState restored = mapper.fromDocument(mapper.toDocument(original));

        assertEquals(original, restored);
    }

    @Test
    void acceptanceFlowRoutesBuildThroughPathAndBuildingPorts() {
        ActionDefinition move = action("move_to_wall", ActionType.MOVE_TO, 1, Set.of(), Set.of(new StateFact("at_wall", true)));
        ActionDefinition place = action("place_wall", ActionType.PLACE_BLOCK, 2,
                Set.of(new StateFact("at_wall", true)), Set.of(new StateFact("wall_built", true)));
        Plan plan = new Plan(UUID.fromString("00000000-0000-0000-0000-000000000013"),
                new Goal(GoalType.BUILD, 75, "wall_built"), List.of(move, place), 3, 0);
        RecordingPorts ports = new RecordingPorts();
        DefaultGoalAiService service = service(ports, plan,
                new ExecutionResult(ExecutionResult.Status.STARTED, ""),
                new ExecutionResult(ExecutionResult.Status.COMPLETED, ""),
                new ExecutionResult(ExecutionResult.Status.STARTED, ""),
                new ExecutionResult(ExecutionResult.Status.COMPLETED, ""));

        DecisionResult first = service.tick(WORLD, CITIZEN, 0, null);
        DecisionResult second = service.tick(WORLD, CITIZEN, 1, sameState(1));
        DecisionResult third = service.tick(WORLD, CITIZEN, 2, sameState(2));
        DecisionResult fourth = service.tick(WORLD, CITIZEN, 3, state(Set.of(new StateFact("at_wall", true)), 3));

        assertEquals(DecisionResult.Status.ACTION_DISPATCHED, first.status());
        assertEquals(DecisionResult.Status.ACTION_COMPLETED, second.status());
        assertEquals(DecisionResult.Status.ACTION_DISPATCHED, third.status());
        assertEquals(DecisionResult.Status.ACTION_COMPLETED, fourth.status());
        assertEquals(1, ports.pathRequests.size());
        assertEquals(1, ports.buildIntents.size());
        assertEquals(List.of("move_to_wall", "place_wall"), ports.actions);
    }

    @Test
    void timeoutCancelsIntentAndPublishesTypedFailure() {
        RecordingPorts ports = new RecordingPorts();
        ActionDefinition action = action("move", ActionType.MOVE_TO, 1, Set.of(), Set.of());
        Plan plan = new Plan(UUID.randomUUID(), new Goal(GoalType.WORK, 70, "done"), List.of(action), 1, 0);
        DefaultGoalAiService service = service(ports, plan,
                new ExecutionResult(ExecutionResult.Status.STARTED, ""));
        service.tick(WORLD, CITIZEN, 0, null);

        DecisionResult result = service.tick(WORLD, CITIZEN, 200, sameState(200));

        assertEquals(DecisionResult.Status.TIMED_OUT, result.status());
        assertEquals(1, ports.cancelledActions.size());
        assertEquals(1, ports.failures.get());
    }

    @Test
    void repeatedPlanningIsBoundedByConfig() {
        RecordingPorts ports = new RecordingPorts();
        AtomicInteger planCalls = new AtomicInteger();
        GOAPPlanner planner = (goal, state) -> {
            planCalls.incrementAndGet();
            return Optional.empty();
        };
        DefaultGoalAiService service = new DefaultGoalAiService(
                (world, citizen, tick) -> state(Set.of(), tick),
                state -> new Goal(GoalType.WORK, 70, "done"), planner,
                (action, state, tick) -> ExecutionResult.failed("unavailable"), new BasicPlanMonitor(),
                (previous, current, plan) -> false, new BoundedFailureHandler(1), ports,
                ports, ports, ports, new InMemoryCitizenAIRepository(), GoalAiConfig::defaults, 1);

        assertEquals(DecisionResult.Status.PLAN_FAILED, service.tick(WORLD, CITIZEN, 0, null).status());
        assertEquals(DecisionResult.Status.PLAN_FAILED, service.tick(WORLD, CITIZEN, 1, sameState(1)).status());
        assertEquals(1, planCalls.get());
    }

    private static DefaultGoalAiService service(RecordingPorts ports, Plan plan, ExecutionResult... results) {
        AtomicInteger index = new AtomicInteger();
        ActionExecutor executor = (action, state, tick) -> results[Math.min(index.getAndIncrement(), results.length - 1)];
        return new DefaultGoalAiService(
                (world, citizen, tick) -> state(tick >= 2 ? Set.of(new StateFact("at_wall", true)) : Set.of(), tick),
                state -> plan.goal(), (goal, state) -> Optional.of(plan), executor, new BasicPlanMonitor(),
                (previous, current, currentPlan) -> false, new BoundedFailureHandler(2), ports,
                ports, ports, ports, new InMemoryCitizenAIRepository(), GoalAiConfig::defaults, 3);
    }

    private static WorldState sameState(long tick) { return state(Set.of(), tick); }

    private static WorldState state(Set<StateFact> facts, long tick) {
        return new WorldState(WORLD, CITIZEN, START, Optional.of(TARGET), facts, tick);
    }

    private static ActionDefinition action(String id, ActionType type, int cost, Set<StateFact> preconditions, Set<StateFact> effects) {
        return new ActionDefinition(id, type, cost, 1, preconditions, effects);
    }

    private static final class RecordingPorts implements GoalActionIntentPort, PathfindingIntentPort,
            BuildTaskIntentPort, GoalAiEventPort {
        private final List<String> actions = new ArrayList<>();
        private final List<PathRequest> pathRequests = new ArrayList<>();
        private final List<BuildTaskIntent> buildIntents = new ArrayList<>();
        private final List<String> cancelledActions = new ArrayList<>();
        private final AtomicInteger failures = new AtomicInteger();

        @Override public void submit(ActionDefinition action, WorldState state) { actions.add(action.id()); }
        @Override public void cancel(String executionId) { cancelledActions.add(executionId); }
        @Override public void request(PathRequest request) { pathRequests.add(request); }
        @Override public void cancel(UUID requestId) { }
        @Override public void submit(BuildTaskIntent intent) { buildIntents.add(intent); }
        @Override public void publishPlanFailure(PlanFailureEvent event) { failures.incrementAndGet(); }
    }
}
