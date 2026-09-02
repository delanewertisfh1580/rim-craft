package com.rimworldcraft.core.ports.driven;
import com.rimworldcraft.core.building.BuildOrder;
import java.util.Optional;
import java.util.UUID;
public interface BuildOrderRepository { Optional<BuildOrder> find(UUID colonyId,UUID orderId); void save(BuildOrder order); }
