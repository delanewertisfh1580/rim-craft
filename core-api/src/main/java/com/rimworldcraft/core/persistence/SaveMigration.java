package com.rimworldcraft.core.persistence;

public interface SaveMigration { String aggregateType(); int fromVersion(); int toVersion(); SaveDocument migrate(SaveDocument source); }
