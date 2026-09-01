package com.rimworldcraft.infrastructure.forge.entity;

import com.rimworldcraft.core.api.ports.IEntitySpawnPort;
import com.rimworldcraft.core.api.types.Position;
import java.util.Objects;
import java.util.UUID;

/** Server-side citizen entity seam; the Forge subclass belongs to the selected mappings. */
public class EntityCitizen {
    private final UUID citizenId;
    private Position position;
    private long lastSyncTime;
    private int cachedMood = 50;
    private String cachedTask = "";
    /** Creates a citizen entity seam. */ public EntityCitizen(UUID citizenId, Position position) { this.citizenId=Objects.requireNonNull(citizenId); this.position=Objects.requireNonNull(position); }
    /** Performs a synchronization tick against the entity port. */ public void tick(IEntitySpawnPort entityPort) { Objects.requireNonNull(entityPort); entityPort.getEntityPosition(citizenId).ifPresent(value -> position=value); lastSyncTime++; }
    /** Returns citizen ID. */ public UUID getCitizenId(){return citizenId;} /** Returns current position. */ public Position getPosition(){return position;} /** Returns cached mood. */ public int getCachedMood(){return cachedMood;} /** Returns cached task. */ public String getCachedTask(){return cachedTask;}
    /** Updates cached render state. */ public void updateRenderState(int mood,String task){cachedMood=Math.max(0,Math.min(100,mood));cachedTask=task==null?"":task;}
    /** Saves the stable identifier into an abstract tag. */ public void saveAdditionalSaveData(Object tag){throw new UnsupportedOperationException("Bind to Forge CompoundTag");}
    /** Loads the stable identifier from an abstract tag. */ public void loadAdditionalSaveData(Object tag){throw new UnsupportedOperationException("Bind to Forge CompoundTag");}
}
