package com.rimworldcraft.core.events;

import com.rimworldcraft.core.shared.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventBusTest {
    private final WorldId world=new WorldId(UUID.randomUUID());
    private EventEnvelope event(Object payload,String type,long sequence){return new EventEnvelope(UUID.randomUUID(),type,new SchemaVersion(1),Instant.EPOCH,world,"corr",payload,"colony/1",sequence);}
    @Test void synchronousDeliveryAndUnsubscribe(){InMemoryEventBus bus=new InMemoryEventBus();AtomicInteger count=new AtomicInteger();Subscription sub=bus.subscribe("player","ColonyFounded",e->count.incrementAndGet());bus.publish(event(new TypedEventContracts.ColonyFounded(world,new ColonyId(UUID.randomUUID())),"ColonyFounded",0));assertEquals(1,count.get());sub.close();bus.publish(event(new TypedEventContracts.ColonyFounded(world,new ColonyId(UUID.randomUUID())),"ColonyFounded",1));assertEquals(1,count.get());}
    @Test void failingHandlerIsRetriedAndDeadLetteredWithoutBlockingOther(){InMemoryEventBus bus=new InMemoryEventBus(new EventDeliveryPolicy(1),Set.of(1));AtomicInteger good=new AtomicInteger();bus.subscribe("bad","WorkAssigned",e->{throw new IllegalStateException("broken");});bus.subscribe("good","WorkAssigned",e->good.incrementAndGet());List<DeadLetter> result=bus.publish(event(new TypedEventContracts.WorkAssigned(world,new ColonyId(UUID.randomUUID()),new CitizenId(UUID.randomUUID()),"build"),"WorkAssigned",0));assertEquals(1,good.get());assertEquals(1,result.size());assertEquals(2,result.get(0).attempts());}
    @Test void idempotencyAndOrderingAreEnforced(){InMemoryEventBus bus=new InMemoryEventBus();AtomicInteger count=new AtomicInteger();bus.subscribe("npc","JobCompleted",e->count.incrementAndGet());EventEnvelope first=event(new TypedEventContracts.JobCompleted(world,new ColonyId(UUID.randomUUID()),new CitizenId(UUID.randomUUID()),"build"),"JobCompleted",0);bus.publish(first);bus.publish(first);assertEquals(1,count.get());assertEquals(1,bus.publish(event(first.payload(),"JobCompleted",2)).size());}
    @Test void unknownSchemaGoesToDeadLetter(){InMemoryEventBus bus=new InMemoryEventBus();List<DeadLetter> result=bus.publish(new EventEnvelope(UUID.randomUUID(),"NpcDied",new SchemaVersion(2),Instant.EPOCH,world,"corr",new TypedEventContracts.NpcDied(world,new CitizenId(UUID.randomUUID()),null)));assertEquals(1,result.size());assertTrue(result.get(0).reason().contains("unknown"));}
    @Test void typedMandatoryPayloadsConstruct(){assertNotNull(new TypedEventContracts.RaidGenerated(world,new ColonyId(UUID.randomUUID()),new IncidentId(UUID.randomUUID())));}
}
