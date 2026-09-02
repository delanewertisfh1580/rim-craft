package com.rimworldcraft.core.npc;

import com.rimworldcraft.core.npc.domain.Citizen;
import com.rimworldcraft.core.npc.domain.CitizenStatus;
import com.rimworldcraft.core.npc.domain.HealthState;
import com.rimworldcraft.core.npc.domain.JobAssignment;
import com.rimworldcraft.core.npc.domain.MoodState;
import com.rimworldcraft.core.npc.domain.Need;
import com.rimworldcraft.core.npc.domain.NeedState;
import com.rimworldcraft.core.npc.domain.NeedType;
import com.rimworldcraft.core.npc.domain.Schedule;
import com.rimworldcraft.core.npc.domain.SkillSet;
import com.rimworldcraft.core.npc.domain.SkillType;
import com.rimworldcraft.core.npc.domain.TraitSet;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.WorldId;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcContextTest {
    private final WorldId world = new WorldId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private Citizen citizen() {
        Map<NeedType, Need> needs = new EnumMap<>(NeedType.class);
        needs.put(NeedType.HUNGER, new Need(NeedType.HUNGER, 80, 1, 20));
        return Citizen.create(new CitizenId(UUID.randomUUID()), world, "Ada", null, new NeedState(needs),
                new MoodState(70, 0), new HealthState(100, 100), new TraitSet(Map.of("optimist", 5), Map.of(SkillType.BUILDING, 2)),
                new SkillSet(Map.of(SkillType.BUILDING, 0), Map.of(SkillType.BUILDING, 0)), Schedule.empty());
    }
    @Test void decayIsDeterministic() { Citizen c = citizen(); c.decayNeeds(10); assertEquals(70, c.needs().require(NeedType.HUNGER).value()); }
    @Test void moodUsesTraits() { Citizen c = citizen(); c.changeMood(c.traits().moodModifier(), 5); assertEquals(75, c.mood().value()); }
    @Test void skillProgressionIsDeterministic() { Citizen c = citizen(); c.gainSkill(SkillType.BUILDING, 10); assertEquals(1, c.skills().level(SkillType.BUILDING)); }
    @Test void incapacitatedAndDeadCannotTakeJobs() { Citizen c = citizen(); c.incapacitate(); assertThrows(IllegalStateException.class, () -> c.assignJob(new JobAssignment(UUID.randomUUID(), "build", world, new GridPosition(0, 0, 0), 1))); c.die(); assertEquals(CitizenStatus.DEAD, c.status()); assertThrows(IllegalStateException.class, () -> c.assignJob(new JobAssignment(UUID.randomUUID(), "build", world, new GridPosition(0, 0, 0), 1))); }
    @Test void jobLifecycleClearsAssignment() { Citizen c = citizen(); JobAssignment j = new JobAssignment(UUID.randomUUID(), "build", world, new GridPosition(0, 0, 0), 1); c.assignJob(j); c.completeJob(2); assertTrue(c.assignment().isEmpty()); }
    @Test void worldMismatchIsRejected() { Citizen c = citizen(); WorldId other = new WorldId(UUID.randomUUID()); assertThrows(IllegalArgumentException.class, () -> c.assignJob(new JobAssignment(UUID.randomUUID(), "build", other, new GridPosition(0, 0, 0), 1))); }
}
