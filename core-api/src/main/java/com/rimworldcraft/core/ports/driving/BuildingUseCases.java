package com.rimworldcraft.core.ports.driving;
import com.rimworldcraft.core.building.BuildOrder;
import com.rimworldcraft.core.api.types.GridPosition;
import java.util.UUID;
public interface BuildingUseCases {
    BuildOrder create(UUID colonyId,UUID orderId,String blueprintId,GridPosition target,int priority);
    void reserve(UUID colonyId,UUID orderId);
    void assignCitizen(UUID colonyId,UUID orderId,UUID citizenId);
    void advance(UUID colonyId,UUID orderId,int progress);
    void complete(UUID colonyId,UUID orderId);
    void cancel(UUID colonyId,UUID orderId);
    void fail(UUID colonyId,UUID orderId);
}
