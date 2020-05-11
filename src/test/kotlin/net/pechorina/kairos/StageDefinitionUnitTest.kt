package net.pechorina.kairos

import net.pechorina.kairos.core.IOLaneType
import net.pechorina.kairos.core.StageDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StageDefinitionUnitTest {

    val yaml = """
---
namespace: "core"
instanceName: "EventLogSink_01"
type: "net.pechorina.kairos.core.verticles.sinks.EventLogSink"
inputLanes:
  - type: EVENT
    name: "in_new_definition"
  - type: EVENT
    name: "out_shell"
  - type: EVENT
    name: in_get_definition
  - type: EVENT
    name: out_get_definition
"""

    @Test
    fun testDeserialize() {
        val def = StageDefinition.fromYaml(yaml)
        assertThat(def).isNotNull
        assertThat(def.namespace).isEqualTo("core")
        assertThat(def.instanceName).isEqualTo("EventLogSink_01")
        assertThat(def.type).isEqualTo("net.pechorina.kairos.core.verticles.sinks.EventLogSink")
        assertThat(def.inputLanes).extracting("type").containsOnly(IOLaneType.EVENT)
    }
}