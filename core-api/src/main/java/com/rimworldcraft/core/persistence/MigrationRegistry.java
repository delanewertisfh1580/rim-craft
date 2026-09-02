package com.rimworldcraft.core.persistence;

import java.util.ArrayList;
import java.util.List;

public final class MigrationRegistry {
    private final List<SaveMigration> migrations=new ArrayList<>();
    public void register(SaveMigration migration){migrations.add(java.util.Objects.requireNonNull(migration));}
    public SaveDocument migrateTo(SaveDocument source,int currentVersion){
        if(source.schemaVersion().value()>currentVersion) throw new IllegalArgumentException("unsupported future schema version");
        SaveDocument current=source;
        while(current.schemaVersion().value()<currentVersion){
            String type=current.aggregateType(); int version=current.schemaVersion().value();
            SaveMigration step=migrations.stream().filter(m->m.aggregateType().equals(type)&&m.fromVersion()==version).findFirst().orElseThrow(()->new IllegalArgumentException("missing migration"));
            current=step.migrate(current);
            if(current.schemaVersion().value() <= version) throw new IllegalArgumentException("migration did not advance schema");
        }
        return current;
    }
}
