package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.player.PlayerEvent;

/** Publishes Player Context facts without exposing aggregate implementations. */
public interface PlayerEventPort {
    void publish(PlayerEvent event);
}
