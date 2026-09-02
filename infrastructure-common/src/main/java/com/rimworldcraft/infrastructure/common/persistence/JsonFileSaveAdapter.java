package com.rimworldcraft.infrastructure.common.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rimworldcraft.core.persistence.SaveDocument;
import com.rimworldcraft.core.persistence.SaveKey;
import com.rimworldcraft.core.ports.driven.JsonSaveLoadPort;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

public final class JsonFileSaveAdapter implements JsonSaveLoadPort {
    private final Path root; private final ObjectMapper mapper; private final Path quarantine; private final Path lastGood;
    public JsonFileSaveAdapter(Path root,ObjectMapper mapper){this.root=root;this.mapper=mapper;this.quarantine=root.resolve("quarantine");this.lastGood=root.resolve("last-known-good");}
    @Override public synchronized Optional<SaveDocument> load(SaveKey key){Path file=file(key); if(!Files.exists(file)) return loadFallback(key); try{return Optional.of(mapper.readValue(file.toFile(),SaveDocument.class));}catch(Exception ex){quarantine(file);return loadFallback(key);}}
    @Override public synchronized void save(SaveKey key,SaveDocument document){try{Files.createDirectories(file(key).getParent());Files.createDirectories(fallback(key).getParent());Path temp=Files.createTempFile(file(key).getParent(),".save-",".tmp");mapper.writeValue(temp.toFile(),document);Files.move(temp,file(key),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);Files.copy(file(key),fallback(key),StandardCopyOption.REPLACE_EXISTING);}catch(IOException ex){throw new IllegalStateException("atomic save failed",ex);}}
    @Override public synchronized void delete(SaveKey key){try{Files.deleteIfExists(file(key));}catch(IOException ex){throw new IllegalStateException("delete failed",ex);}}
    private Path file(SaveKey key){return root.resolve(key.logicalPath()+".json");} private Path fallback(SaveKey key){return lastGood.resolve(key.logicalPath()+".json");}
    private Optional<SaveDocument> loadFallback(SaveKey key){try{Path f=fallback(key);return Files.exists(f)?Optional.of(mapper.readValue(f.toFile(),SaveDocument.class)):Optional.empty();}catch(IOException ex){return Optional.empty();}}
    private void quarantine(Path file){try{if(Files.exists(file)){Files.createDirectories(quarantine);Files.move(file,quarantine.resolve(file.getFileName()+"."+System.nanoTime()+".corrupt"),StandardCopyOption.REPLACE_EXISTING);}}catch(IOException ignored){}}
}
