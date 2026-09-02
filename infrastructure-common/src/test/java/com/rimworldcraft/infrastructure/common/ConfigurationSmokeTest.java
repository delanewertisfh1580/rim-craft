package com.rimworldcraft.infrastructure.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.rimworldcraft.infrastructure.common.config.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationSmokeTest {
    @Test void negativeAndBoundaryValuesAreCheckedBySchema(){JsonSchemaValidator v=new JsonSchemaValidator();String schema="{\"type\":\"integer\",\"minimum\":0,\"maximum\":10}";assertThat(v.validate("-1",schema)).isNotEmpty();assertThat(v.validate("10",schema)).isEmpty();}
    @Test void corruptFileIsRejected(){assertThat(new JsonSchemaValidator().validate("not-json","{\"type\":\"object\"}")).isNotEmpty();}
    @Test void schemaRejectsMissingFieldAndWrongType(){JsonSchemaValidator v=new JsonSchemaValidator();String schema="{\"type\":\"object\",\"required\":[\"schemaVersion\",\"data\"],\"properties\":{\"schemaVersion\":{\"type\":\"integer\",\"minimum\":1},\"data\":{\"type\":\"object\"}}}";assertThat(v.validate("{\"data\":{}}",schema)).isNotEmpty();assertThat(v.validate("{\"schemaVersion\":\"x\",\"data\":{}}",schema)).isNotEmpty();}
    @Test void mutatorDoesNotChangeOriginal(){String original="{\"schemaVersion\":1,\"data\":{\"limit\":2}}";String mutated=new ConfigMutator().replaceNumber(original,"data.limit",BigDecimal.valueOf(-1));assertThat(original).contains("2");assertThat(mutated).contains("-1");}
    @Test void semanticValidatorRejectsDuplicateAndUnknownReference(){JsonNode node=com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().createObjectNode().set("items",com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().createArrayNode().add(com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().createObjectNode().put("id","a").put("ref","missing")).add(com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().createObjectNode().put("id","a")));assertThat(new ConfigSemanticValidator().validate(node,"items","ref",Set.of("known"))).hasSize(2);}
    @Test void snapshotAndPublisherAreImmutableByReplacement(){ConfigSnapshot snapshot=new ConfigSnapshot(1,Map.of("npc",com.rimworldcraft.infrastructure.common.util.JsonUtils.mapper().createObjectNode()),"default");AtomicConfigPublisher publisher=new AtomicConfigPublisher();publisher.publish(snapshot);assertThat(publisher.current()).isEqualTo(snapshot);}
}
