package com.rimworldcraft.infrastructure.common.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small filesystem utility used by infrastructure adapters. */
public final class FileUtils {
    private FileUtils() { }
    /** Creates a directory and all missing parents. */
    public static void createDirectory(Path path) { try { Files.createDirectories(path); } catch (IOException exception) { throw new IllegalStateException("Cannot create directory: " + path, exception); } }
    /** Writes UTF-8 content, creating parent directories first. */
    public static void writeStringToFile(Path path, String content) { try { if (path.getParent() != null) Files.createDirectories(path.getParent()); Files.writeString(path, content, StandardCharsets.UTF_8); } catch (IOException exception) { throw new IllegalStateException("Cannot write file: " + path, exception); } }
    /** Reads UTF-8 content. */
    public static String readStringFromFile(Path path) { try { return Files.readString(path, StandardCharsets.UTF_8); } catch (IOException exception) { throw new IllegalStateException("Cannot read file: " + path, exception); } }
    /** Returns whether a regular file exists. */
    public static boolean fileExists(Path path) { return Files.isRegularFile(path); }
}
