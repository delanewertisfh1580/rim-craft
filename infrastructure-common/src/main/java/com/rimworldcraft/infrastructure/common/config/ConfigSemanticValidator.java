package com.rimworldcraft.infrastructure.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

public final class ConfigSemanticValidator {
    public Set<String> validate(JsonNode document, String idArray, String referenceField, Set<String> knownIds) {
        Set<String> errors=new HashSet<>(); Set<String> ids=new HashSet<>(); JsonNode items=document.path(idArray);
        if(items.isArray()) for(JsonNode item:items){String id=item.path("id").asText(""); if(!ids.add(id)) errors.add("duplicate ID: "+id); if(referenceField!=null&&!referenceField.isBlank()){String ref=item.path(referenceField).asText(""); if(!ref.isBlank()&&!knownIds.contains(ref)) errors.add("unknown reference: "+ref);}}
        return Set.copyOf(errors);
    }
}
