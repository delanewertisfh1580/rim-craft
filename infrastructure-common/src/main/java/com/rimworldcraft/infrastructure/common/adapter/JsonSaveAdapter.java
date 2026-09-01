package com.rimworldcraft.infrastructure.common.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rimworldcraft.core.api.ports.ISaveLoadPort;
import com.rimworldcraft.infrastructure.common.util.FileUtils;
import com.rimworldcraft.infrastructure.common.util.JsonUtils;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** File-backed JSON implementation of the save/load port. */
public final class JsonSaveAdapter implements ISaveLoadPort {
    private final Path saveDirectory;
    private final ObjectMapper mapper;
    private final Set<UUID> knownColonies = ConcurrentHashMap.newKeySet();
    /** Creates a save adapter rooted at the supplied directory. */
    public JsonSaveAdapter(Path saveDirectory) { this.saveDirectory = Objects.requireNonNull(saveDirectory); this.mapper = JsonUtils.mapper(); FileUtils.createDirectory(saveDirectory); }
    /** Saves a minimal colony marker document. Repository hydration is owned by repositories. */
    @Override public synchronized void saveColony(UUID colonyId) { Objects.requireNonNull(colonyId); Path directory = colonyDirectory(colonyId); FileUtils.createDirectory(directory); FileUtils.writeStringToFile(directory.resolve("colony.json"), "{\"colonyId\":\"" + colonyId + "\"}"); knownColonies.add(colonyId); }
    /** Loads a colony marker when its save exists. */
    @Override public synchronized void loadColony(UUID colonyId) { Objects.requireNonNull(colonyId); if (!exists(colonyId)) throw new IllegalStateException("Colony save does not exist: " + colonyId); knownColonies.add(colonyId); }
    /** Returns whether a colony save directory exists. */
    @Override public synchronized boolean exists(UUID colonyId) { return Files.isDirectory(colonyDirectory(Objects.requireNonNull(colonyId))); }
    /** Deletes a colony directory recursively. */
    @Override public synchronized void deleteColony(UUID colonyId) { Path directory = colonyDirectory(Objects.requireNonNull(colonyId)); if (!Files.exists(directory)) return; try (var paths = Files.walk(directory)) { paths.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception exception) { throw new IllegalStateException("Cannot delete save: " + path, exception); } }); } catch (Exception exception) { throw new IllegalStateException("Cannot delete colony save: " + colonyId, exception); } knownColonies.remove(colonyId); }
    /** Flushes pending state; individual writes are already durable. */
    @Override public synchronized void saveAll() { FileUtils.createDirectory(saveDirectory); }
    /** Discovers all existing colony directories. */
    @Override public synchronized void loadAll() { try (var paths = Files.list(saveDirectory)) { paths.filter(Files::isDirectory).forEach(path -> { try { knownColonies.add(UUID.fromString(path.getFileName().toString())); } catch (IllegalArgumentException ignored) { } }); } catch (Exception exception) { throw new IllegalStateException("Cannot scan save directory", exception); } }
    /** Returns the root save directory. */ public Path getSaveDirectory() { return saveDirectory; }
    private Path colonyDirectory(UUID id) { return saveDirectory.resolve("colonies").resolve(id.toString()); }
}
