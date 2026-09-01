package com.rimworldcraft.infrastructure.common;

/** Serialization helper seam; concrete Minecraft tags belong in platform modules. */
public final class NbtHelper {
    private NbtHelper() { }
    public static void putString(Object tag, String key, String value) { throw new UnsupportedOperationException("Implement platform NBT mapping"); }
    public static String getString(Object tag, String key) { throw new UnsupportedOperationException("Implement platform NBT mapping"); }
}
