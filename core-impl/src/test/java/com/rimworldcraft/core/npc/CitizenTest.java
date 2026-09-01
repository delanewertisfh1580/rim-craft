package com.rimworldcraft.core.npc;

import com.rimworldcraft.core.api.types.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Unit tests for Citizen aggregate. */
class CitizenTest {
    private Citizen citizen(){return new Citizen(UUID.randomUUID(),"Alex",Gender.UNKNOWN,"human",UUID.randomUUID(),new Position(0,64,0));}
    @Test void changeMood_shouldClampValue(){Citizen citizen=citizen();citizen.changeMood(100,"joy");assertThat(citizen.getMood().value()).isEqualTo(100);}
    @Test void addSkillExperience_shouldIncreaseExperience(){Citizen citizen=citizen();citizen.addSkillExperience(SkillType.BUILDING,120);assertThat(citizen.getSkills().get(SkillType.BUILDING).level()).isGreaterThan(0);}
    @Test void die_shouldMarkCitizenDead(){Citizen citizen=citizen();citizen.die("test");assertThat(citizen.isAlive()).isFalse();}
}
