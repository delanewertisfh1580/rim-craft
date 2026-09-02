package com.rimworldcraft.infrastructure.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.Objects;

public final class ConfigMutator {
    public String removeRequiredField(String json, String path) { ObjectNode root=(ObjectNode) parse(json); parent(root,path).remove(last(path)); return write(root); }
    public String replaceNumber(String json, String path, BigDecimal value) { ObjectNode root=(ObjectNode) parse(json); parent(root,path).put(last(path),value); return write(root); }
    public String replaceString(String json, String path, String value) { ObjectNode root=(ObjectNode) parse(json); parent(root,path).put(last(path),Objects.requireNonNull(value)); return write(root); }
    private JsonNode parse(String json){try{return com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().readTree(json);}catch(Exception e){throw new IllegalArgumentException("invalid JSON",e);}}
    private ObjectNode parent(ObjectNode root,String path){String p=path.substring(0,path.lastIndexOf('.')); JsonNode node=root; for(String part:p.split("\\.")){node=node.path(part);} if(!node.isObject())throw new IllegalArgumentException("parent is not object: "+p); return (ObjectNode)node;}
    private String last(String path){return path.substring(path.lastIndexOf('.')+1);}
    private String write(JsonNode node){try{return com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().writeValueAsString(node);}catch(Exception e){throw new IllegalStateException(e);}}
}
