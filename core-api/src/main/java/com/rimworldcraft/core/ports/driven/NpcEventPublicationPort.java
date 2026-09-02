package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.contracts.NpcJobCompletedEvent;

/** Publishes typed NPC integration events. */
public interface NpcEventPublicationPort { void publishJobCompleted(NpcJobCompletedEvent event); }
