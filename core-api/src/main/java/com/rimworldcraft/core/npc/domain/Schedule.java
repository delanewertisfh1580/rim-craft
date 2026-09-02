package com.rimworldcraft.core.npc.domain;

import java.util.List;

/** Ordered, immutable job schedule. */
public record Schedule(List<String> jobTypes) {
    public Schedule { jobTypes = List.copyOf(jobTypes == null ? List.of() : jobTypes); }
    public static Schedule empty() { return new Schedule(List.of()); }
}
