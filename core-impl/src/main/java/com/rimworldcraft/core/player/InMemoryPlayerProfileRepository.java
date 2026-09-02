package com.rimworldcraft.core.player;

import com.rimworldcraft.core.ports.driven.PlayerProfileRepository;
import com.rimworldcraft.core.shared.PlayerId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** In-memory repository implementation; production persistence remains an adapter concern. */
public final class InMemoryPlayerProfileRepository implements PlayerProfileRepository {
    private final Map<Key, PlayerProfile> profiles = new HashMap<>();

    @Override
    public Optional<PlayerProfile> find(WorldId worldId, PlayerId playerId) {
        return Optional.ofNullable(profiles.get(new Key(worldId, playerId)));
    }

    @Override
    public PlayerProfile save(PlayerProfile profile) {
        Objects.requireNonNull(profile, "profile");
        profiles.put(new Key(profile.worldId(), profile.playerId()), profile);
        return profile;
    }

    private record Key(WorldId worldId, PlayerId playerId) { }
}
