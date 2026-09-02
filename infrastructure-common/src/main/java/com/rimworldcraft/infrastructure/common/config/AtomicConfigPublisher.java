package com.rimworldcraft.infrastructure.common.config;

import java.util.concurrent.atomic.AtomicReference;

public final class AtomicConfigPublisher {
    private final AtomicReference<ConfigSnapshot> current=new AtomicReference<>();
    public ConfigSnapshot current(){return current.get();}
    public ConfigSnapshot publish(ConfigSnapshot candidate){if(candidate==null)throw new IllegalArgumentException("candidate"); current.set(candidate); return candidate;}
}
