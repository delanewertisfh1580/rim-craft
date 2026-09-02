package com.rimworldcraft.core.ports.driven;
import com.rimworldcraft.core.api.types.ResourceType;
import java.util.Map;
import java.util.UUID;
public interface ResourceReservationPort { boolean reserve(UUID orderId,Map<ResourceType,Integer> costs); void release(UUID orderId); }
