package com.rimworldcraft.core.factory;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** Unit tests for CitizenFactory. */
class CitizenFactoryTest {
    @Test void createRandomCitizen_shouldReturnAliveCitizen(){assertThat(new CitizenFactory().createRandomCitizen().isAlive()).isTrue();}
}
