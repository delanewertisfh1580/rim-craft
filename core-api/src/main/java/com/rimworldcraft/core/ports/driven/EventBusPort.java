package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.events.EventEnvelope;
import com.rimworldcraft.core.events.EventHandler;
import com.rimworldcraft.core.events.Subscription;
import java.util.List;

public interface EventBusPort {
    List<com.rimworldcraft.core.events.DeadLetter> publish(EventEnvelope event);
    Subscription subscribe(String handlerId, String eventType, EventHandler handler);
    List<com.rimworldcraft.core.events.DeadLetter> deadLetters();
}
