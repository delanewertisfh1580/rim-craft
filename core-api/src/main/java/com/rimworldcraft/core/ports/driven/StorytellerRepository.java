package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.shared.StorytellerId;
import com.rimworldcraft.core.shared.WorldId;
import com.rimworldcraft.core.storyteller.Storyteller;
import java.util.Optional;

/** Driven repository contract owned by Storyteller Context. */
public interface StorytellerRepository {
    Optional<Storyteller> find(WorldId worldId, StorytellerId storytellerId);
    Storyteller save(Storyteller storyteller);
}
