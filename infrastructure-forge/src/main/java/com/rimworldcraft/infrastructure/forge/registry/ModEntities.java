package com.rimworldcraft.infrastructure.forge.registry;

import com.rimworldcraft.infrastructure.forge.entity.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/** Entity registry seam for Forge registration events. */
public final class ModEntities {
    private final Map<String, Class<?>> types = new ConcurrentHashMap<>();
    /** Registers the default entity classes. */ public void registerDefaults(){types.put("citizen",EntityCitizen.class);types.put("enemy",EntityEnemy.class);types.put("trader",EntityTrader.class);}
    /** Returns registered entity classes. */ public Map<String,Class<?>> types(){return Map.copyOf(types);}
}
