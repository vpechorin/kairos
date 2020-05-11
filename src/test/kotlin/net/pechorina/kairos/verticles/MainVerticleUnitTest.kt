package net.pechorina.kairos.verticles

import io.vertx.core.json.JsonObject
import net.pechorina.kairos.core.utils.ResourceUtils
import net.pechorina.kairos.core.verticles.MainVerticle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MainVerticleUnitTest {

    @Test
    fun testFindBootResource() {
        val bootResources = ResourceUtils.getBootResources()
        assertThat(bootResources).isNotEmpty
    }

    @Test
    fun testLoadBootResource() {
        val mainVerticle = MainVerticle()
        val bootResources = ResourceUtils.getBootResources()
        bootResources.forEach {
            val stageDefinition = mainVerticle.loadStage(it, JsonObject())
            assertThat(stageDefinition).isNotNull
        }
    }
}