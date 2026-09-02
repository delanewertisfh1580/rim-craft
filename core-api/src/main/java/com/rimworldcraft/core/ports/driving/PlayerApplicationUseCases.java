package com.rimworldcraft.core.ports.driving;

/** Combined Player Context application boundary. */
public interface PlayerApplicationUseCases extends RegisterPlayerUseCase, JoinColonyUseCase,
        LeaveColonyUseCase, AuthorizePlayerCommandUseCase, ChangeControlModeUseCase,
        GetPlayerViewUseCase, SelectPlayerTargetUseCase, ChangePlayerPermissionsUseCase { }
