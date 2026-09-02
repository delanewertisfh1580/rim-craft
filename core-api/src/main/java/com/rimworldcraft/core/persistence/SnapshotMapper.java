package com.rimworldcraft.core.persistence;

public interface SnapshotMapper<T> { SaveDocument toDocument(T aggregate); T fromDocument(SaveDocument document); }
