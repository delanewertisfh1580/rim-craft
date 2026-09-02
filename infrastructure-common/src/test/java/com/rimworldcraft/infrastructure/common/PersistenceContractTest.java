package com.rimworldcraft.infrastructure.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rimworldcraft.core.persistence.*;
import com.rimworldcraft.core.shared.WorldId;
import com.rimworldcraft.infrastructure.common.persistence.JsonFileSaveAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PersistenceContractTest {
    private SaveKey key(){return new SaveKey(new WorldId(UUID.randomUUID()),"citizen","one");}
    private SaveDocument doc(SaveKey key){return new SaveDocument("rwc-save",1,"citizen",key.aggregateId(),key.worldId(),new com.rimworldcraft.core.shared.SchemaVersion(1),new AggregateVersion(1),10,Map.of("name","Ada"),Map.of());}
    @Test void jsonRoundTripAndRepeatedLoadsAreStable() throws Exception {Path root=Files.createTempDirectory("rwc-save");JsonFileSaveAdapter adapter=new JsonFileSaveAdapter(root,new ObjectMapper());SaveKey key=key();SaveDocument original=doc(key);adapter.save(key,original);assertThat(adapter.load(key)).contains(original);assertThat(adapter.load(key)).contains(original);}
    @Test void corruptFileFallsBackToLastKnownGoodAndQuarantines() throws Exception {Path root=Files.createTempDirectory("rwc-save");JsonFileSaveAdapter adapter=new JsonFileSaveAdapter(root,new ObjectMapper());SaveKey key=key();SaveDocument original=doc(key);adapter.save(key,original);Path file=root.resolve(key.logicalPath()+".json");Files.writeString(file,"broken");assertThat(adapter.load(key)).contains(original);assertThat(Files.list(root.resolve("quarantine")).findAny()).isPresent();}
    @Test void futureVersionIsRejectedByMigrationRegistry(){SaveDocument future=new SaveDocument("rwc-save",1,"citizen","one",new WorldId(UUID.randomUUID()),new com.rimworldcraft.core.shared.SchemaVersion(2),new AggregateVersion(0),0,Map.of(),Map.of());assertThatThrownBy(()->new MigrationRegistry().migrateTo(future,1)).isInstanceOf(IllegalArgumentException.class);}
}
