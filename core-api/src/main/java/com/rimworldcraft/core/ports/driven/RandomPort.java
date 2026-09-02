package com.rimworldcraft.core.ports.driven;

/** Randomness abstraction allowing seeded deterministic tests and replays. */
public interface RandomPort { int nextInt(int bound); double nextDouble(); }
