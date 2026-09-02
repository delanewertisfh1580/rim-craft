package com.rimworldcraft.core.goal;
import java.util.Objects;
public record StateFact(String key, boolean value) { public StateFact { if(key==null||key.isBlank())throw new IllegalArgumentException("fact key"); } }
