package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.persistence.SaveDocument;
import com.rimworldcraft.core.persistence.SaveKey;
import java.util.Optional;

public interface JsonSaveLoadPort {
    Optional<SaveDocument> load(SaveKey key);
    void save(SaveKey key, SaveDocument document);
    void delete(SaveKey key);
}
