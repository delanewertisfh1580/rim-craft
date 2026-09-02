package com.rimworldcraft.core.events;

public interface Subscription extends AutoCloseable {
    @Override void close();
    boolean active();
}
