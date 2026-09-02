package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.ports.driven.*;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.GameTick;
import com.rimworldcraft.core.shared.WorldId;
import java.util.*;

/** Server-side orchestration for one bounded AI decision step. */
public final class DefaultGoalAiService {
    private final WorldStateObserver observer;
    private final GoalSelector selector;
    private final GOAPPlanner planner;
    private final ActionExecutor executor;
    private final PlanMonitor monitor;
    private final ReplanningTrigger trigger;
    private final FailureHandler failures;
    private final GoalActionIntentPort actionIntents;
    private final PathfindingIntentPort pathIntents;
    private final BuildTaskIntentPort buildIntents;
    private final GoalAiEventPort events;
    private final CitizenAIRepository repository;
    private final GoalConfigPort configPort;
    private final int maxReplans;
    private final Map<CitizenId, CitizenAIState> activeStates = new HashMap<>();

    public DefaultGoalAiService(WorldStateObserver observer, GoalSelector selector, GOAPPlanner planner,
                                ActionExecutor executor, PlanMonitor monitor, ReplanningTrigger trigger,
                                FailureHandler failures, GoalActionIntentPort actionIntents, int maxReplans) {
        this(observer, selector, planner, executor, monitor, trigger, failures, actionIntents,
                new PathfindingIntentPort() {
                    @Override public void request(PathRequest request) { }
                    @Override public void cancel(UUID requestId) { }
                },
                intent -> { },
                event -> { },
                new CitizenAIRepository() {
                    @Override public Optional<CitizenAIState> find(CitizenId id) { return Optional.empty(); }
                    @Override public void save(CitizenAIState state) { }
                },
                () -> GoalAiConfig.defaults(), maxReplans);
    }

    public DefaultGoalAiService(WorldStateObserver observer, GoalSelector selector, GOAPPlanner planner,
                                ActionExecutor executor, PlanMonitor monitor, ReplanningTrigger trigger,
                                FailureHandler failures, GoalActionIntentPort actionIntents,
                                PathfindingIntentPort pathIntents, BuildTaskIntentPort buildIntents,
                                GoalAiEventPort events, CitizenAIRepository repository,
                                GoalConfigPort configPort, int maxReplans) {
        this.observer = Objects.requireNonNull(observer);
        this.selector = Objects.requireNonNull(selector);
        this.planner = Objects.requireNonNull(planner);
        this.executor = Objects.requireNonNull(executor);
        this.monitor = Objects.requireNonNull(monitor);
        this.trigger = Objects.requireNonNull(trigger);
        this.failures = Objects.requireNonNull(failures);
        this.actionIntents = Objects.requireNonNull(actionIntents);
        this.pathIntents = Objects.requireNonNull(pathIntents);
        this.buildIntents = Objects.requireNonNull(buildIntents);
        this.events = Objects.requireNonNull(events);
        this.repository = Objects.requireNonNull(repository);
        this.configPort = Objects.requireNonNull(configPort);
        if (maxReplans < 0) throw new IllegalArgumentException("maxReplans");
        this.maxReplans = maxReplans;
    }

    public DecisionResult tick(WorldId worldId, CitizenId citizenId, long tick, WorldState previous) {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(citizenId, "citizenId");
        if (tick < 0) throw new IllegalArgumentException("tick");
        GoalAiConfig config = configPort.snapshot();
        WorldState state = observer.observe(worldId, citizenId, tick);
        CitizenAIState prior = activeStates.getOrDefault(citizenId, repository.find(citizenId).orElse(null));
        if (prior != null && prior.status() == AIStatus.SUSPENDED) return new DecisionResult(DecisionResult.Status.CANCELLED, prior.replans());
        Goal goal = selector.select(state);
        boolean needPlan = prior == null || prior.plan() == null || prior.goal() == null
                || !prior.goal().equals(goal) || prior.status() != AIStatus.ACTIVE
                || (previous != null && trigger.shouldReplan(previous, state, prior.plan()));
        CitizenAIState current = prior;
        if (needPlan) {
            if (prior != null && prior.replans() >= Math.min(config.maxReplans(), maxReplans)) {
                return fail(worldId, citizenId, prior, tick, "MAX_REPLANS");
            }
            Optional<Plan> planned = planner.plan(goal, state);
            if (planned.isEmpty()) return fail(worldId, citizenId,
                    stateForFailure(worldId, citizenId, goal, tick, prior), tick, "NO_PLAN");
            current = new CitizenAIState(citizenId, worldId, goal, planned.get(), AIStatus.ACTIVE,
                    prior == null ? 0 : prior.replans() + 1, 0, tick, 0, tick);
            activeStates.put(citizenId, current);
            repository.save(current);
        }
        if (current == null || !current.hasNextAction()) return new DecisionResult(DecisionResult.Status.IDLE, 0);
        if (monitor.timedOut(current.actionStartedTick(), tick, config.actionTimeoutTicks())) {
            return fail(worldId, citizenId, current, tick, "ACTION_TIMEOUT");
        }
        ActionDefinition action = current.nextAction();
        if (!monitor.valid(current.plan(), state)
                || !Preconditions.satisfiedBy(action.preconditions(), state)) {
            return new DecisionResult(DecisionResult.Status.REPLAN_REQUIRED, current.replans());
        }
        ExecutionResult result = executor.execute(action, state, tick);
        if (result.status() == ExecutionResult.Status.STARTED) {
            current = current.withActiveAction(tick, current.actionAttempts() + 1);
            activeStates.put(citizenId, current);
            repository.save(current);
            dispatch(worldId, citizenId, action, state);
            return new DecisionResult(DecisionResult.Status.ACTION_DISPATCHED, Optional.empty(), current.replans(), Optional.of(action));
        }
        if (result.status() == ExecutionResult.Status.COMPLETED) {
            current = current.advanceAction(tick);
            activeStates.put(citizenId, current);
            repository.save(current);
            return new DecisionResult(DecisionResult.Status.ACTION_COMPLETED, Optional.empty(), current.replans(), Optional.of(action));
        }
        return fail(worldId, citizenId, current, tick, result.reason());
    }

    public void cancel(CitizenId citizenId) {
        Objects.requireNonNull(citizenId, "citizenId");
        CitizenAIState state = activeStates.get(citizenId);
        if (state != null) {
            actionIntents.cancel(executionId(citizenId, state));
            if (state.plan() != null && state.hasNextAction() && state.nextAction().type() == ActionType.MOVE_TO) {
                pathIntents.cancel(pathRequestId(citizenId, state.nextAction()));
            }
            CitizenAIState suspended = state.suspended();
            activeStates.put(citizenId, suspended);
            repository.save(suspended);
        }
    }

    private void dispatch(WorldId worldId, CitizenId citizenId, ActionDefinition action, WorldState state) {
        actionIntents.submit(action, state);
        if (action.type() == ActionType.MOVE_TO) {
            state.targetPosition().ifPresent(target -> pathIntents.request(new PathRequest(
                    pathRequestId(citizenId, action), worldId, citizenId, state.position(), target)));
        }
        if (action.type() == ActionType.PLACE_BLOCK && state.targetPosition().isPresent()) {
            buildIntents.submit(new BuildTaskIntentPort.BuildTaskIntent(
                    UUID.nameUUIDFromBytes((citizenId + ":" + action.id()).getBytes()),
                    UUID.nameUUIDFromBytes(action.id().getBytes()), worldId, citizenId, state.targetPosition().orElseThrow()));
        }
    }

    private static UUID pathRequestId(CitizenId citizenId, ActionDefinition action) {
        return UUID.nameUUIDFromBytes((citizenId + ":" + action.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String executionId(CitizenId citizenId, CitizenAIState state) {
        return citizenId + ":" + (state.hasNextAction() ? state.nextAction().id() : "none");
    }

    private CitizenAIState stateForFailure(WorldId worldId, CitizenId citizenId, Goal goal, long tick, CitizenAIState prior) {
        return new CitizenAIState(citizenId, worldId, goal, null, AIStatus.FAILED,
                prior == null ? 1 : prior.replans() + 1, 0, tick, 0, tick);
    }

    private DecisionResult fail(WorldId worldId, CitizenId citizenId, CitizenAIState state, long tick, String reason) {
        FailureHandler.Decision decision = failures.onFailure(reason, state.actionAttempts());
        if (state.plan() != null && state.hasNextAction()) {
            actionIntents.cancel(executionId(citizenId, state));
            if (state.nextAction().type() == ActionType.MOVE_TO) {
                pathIntents.cancel(pathRequestId(citizenId, state.nextAction()));
            }
        }
        PlanFailure failure = new PlanFailure(state.plan() == null ? UUID.nameUUIDFromBytes((reason + citizenId).getBytes()) : state.plan().id(), citizenId, reason, state.actionAttempts());
        events.publishPlanFailure(new PlanFailureEvent(failure, new GameTick(tick)));
        CitizenAIState next = new CitizenAIState(citizenId, worldId, state.goal(), state.plan(),
                decision == FailureHandler.Decision.IDLE ? AIStatus.IDLE : AIStatus.FAILED,
                state.replans(), state.nextActionIndex(), state.actionStartedTick(), state.actionAttempts() + 1, tick);
        activeStates.put(citizenId, next);
        repository.save(next);
        return new DecisionResult(reason.contains("TIMEOUT") ? DecisionResult.Status.TIMED_OUT : DecisionResult.Status.PLAN_FAILED,
                Optional.of(failure), state.replans(), Optional.empty());
    }
}
