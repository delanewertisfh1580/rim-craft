package com.rimworldcraft.test;

import com.rimworldcraft.core.aggregate.Colony;
import com.rimworldcraft.core.api.*;
import java.util.UUID;

/** Shared fixtures; see testing-strategy.md. */
public final class TestDataFactory {
    private TestDataFactory() { }
    public static Colony colony() { return new Colony(new ColonyId(UUID.randomUUID()), "Test Colony"); }
    public static Position position() { return new Position(0, 64, 0); }
}
