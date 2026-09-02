package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.ports.driven.StorytellerRepository;
import com.rimworldcraft.core.shared.StorytellerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministic repository implementation for Storyteller contract tests. */
public final class InMemoryStorytellerRepository implements StorytellerRepository {
    private final Map<String, Storyteller> values = new HashMap<>();

    @Override
    public Optional<Storyteller> find(WorldId worldId, StorytellerId storytellerId) {
        return Optional.ofNullable(values.get(key(worldId, storytellerId)));
    }

    @Override
    public Storyteller save(Storyteller storyteller) {
        Objects.requireNonNull(storyteller, "storyteller");
        values.put(key(storyteller.worldId(), storyteller.id()), storyteller);
        return storyteller;
    }

    private static String key(WorldId worldId, StorytellerId storytellerId) {
        return worldId.value() + ":" + storytellerId.value();
    }
}
