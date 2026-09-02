package com.rimworldcraft.core.storyteller;

import com.rimworldcraft.core.shared.IncidentId;
import java.util.Objects;

/** Result reported by an external incident executor. */
public record IncidentOutcome(IncidentId incidentId, boolean successful, int pressureDelta, String resultCode) {
    public IncidentOutcome {
        Objects.requireNonNull(incidentId, "incidentId");
        if (pressureDelta < -100 || pressureDelta > 100) throw new IllegalArgumentException("pressure delta");
        if (resultCode == null || resultCode.isBlank()) throw new IllegalArgumentException("resultCode");
    }
}
