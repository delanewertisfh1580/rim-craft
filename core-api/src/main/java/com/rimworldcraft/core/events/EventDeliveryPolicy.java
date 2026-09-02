package com.rimworldcraft.core.events;

public record EventDeliveryPolicy(int maxRetries) {
    public EventDeliveryPolicy { if (maxRetries < 0 || maxRetries > 10) throw new IllegalArgumentException("maxRetries must be 0..10"); }
    public static EventDeliveryPolicy noRetry() { return new EventDeliveryPolicy(0); }
}
