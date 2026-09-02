package com.rimworldcraft.core.events;

import com.rimworldcraft.core.ports.driven.EventBusPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** In-process synchronous event bus for JVM tests and single-process MVP runtime. */
public final class InMemoryEventBus implements EventBusPort {
    private record Registration(String id, String type, EventHandler handler) {}
    private final Map<String, List<Registration>> registrations = new ConcurrentHashMap<>();
    private final Set<String> processed = ConcurrentHashMap.newKeySet();
    private final List<DeadLetter> deadLetters = new ArrayList<>();
    private final Map<String, Long> lastSequence = new ConcurrentHashMap<>();
    private final EventDeliveryPolicy policy; private final Set<Integer> supportedSchemas;

    public InMemoryEventBus(EventDeliveryPolicy policy, Set<Integer> supportedSchemas) {
        this.policy=Objects.requireNonNull(policy); this.supportedSchemas=Set.copyOf(supportedSchemas);
    }
    public InMemoryEventBus() { this(EventDeliveryPolicy.noRetry(), Set.of(1)); }
    @Override public synchronized List<DeadLetter> publish(EventEnvelope event) {
        Objects.requireNonNull(event, "event");
        if (!supportedSchemas.contains(event.schemaVersion().value())) return List.of(record(event, "schema", 0, "unknown schema version"));
        long expected=lastSequence.getOrDefault(event.aggregateStream(), -1L)+1;
        if (event.sequence() != expected) return List.of(record(event, "ordering", 0, "aggregate stream out of order"));
        lastSequence.put(event.aggregateStream(), event.sequence());
        List<DeadLetter> failures=new ArrayList<>();
        for (Registration registration : List.copyOf(registrations.getOrDefault(event.eventType(), List.of()))) {
            if (processed.contains(key(registration,event))) continue;
            int attempts=0; boolean success=false; Exception failure=null;
            while (!success && attempts <= policy.maxRetries()) { attempts++; try { registration.handler().handle(event); success=true; } catch (Exception ex) { failure=ex; } }
            if(success) processed.add(key(registration,event)); else failures.add(record(event,registration.id(),attempts,failure == null ? "handler failed" : failure.getMessage()));
        }
        return List.copyOf(failures);
    }
    @Override public synchronized Subscription subscribe(String handlerId, String eventType, EventHandler handler) {
        Objects.requireNonNull(handlerId); Objects.requireNonNull(eventType); Objects.requireNonNull(handler);
        if (registrations.values().stream().flatMap(List::stream).anyMatch(r -> r.id().equals(handlerId))) throw new IllegalArgumentException("duplicate handler: " + handlerId);
        Registration registration=new Registration(handlerId,eventType,handler); registrations.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(registration);
        return new Subscription() { private boolean active=true; public void close(){ synchronized(InMemoryEventBus.this){ registrations.getOrDefault(eventType,List.of()).remove(registration); active=false; }} public boolean active(){return active;} };
    }
    @Override public List<DeadLetter> deadLetters(){ synchronized(this){return List.copyOf(deadLetters);} }
    private DeadLetter record(EventEnvelope event,String handler,int attempts,String reason){DeadLetter dl=new DeadLetter(event,handler,attempts,reason==null?"handler failed":reason);deadLetters.add(dl);return dl;}
    private static String key(Registration r,EventEnvelope e){return r.id()+":"+e.eventId();}
}
