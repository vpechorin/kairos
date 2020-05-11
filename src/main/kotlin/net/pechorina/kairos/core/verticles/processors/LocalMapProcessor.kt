package net.pechorina.kairos.core.verticles.processors

import io.vertx.core.shareddata.SharedData
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.Block
import net.pechorina.kairos.core.ConfigKey
import net.pechorina.kairos.core.verticles.StageVerticle

class LocalMapProcessor : StageVerticle() {

    override fun start() {
        super.start()

        val sharedData = vertx.sharedData()
        definition.opts.section(ConfigKey.LocalMap.key)
                .blocks
                .forEach { block -> fillData(sharedData, block) }
    }

    private fun fillData(sharedData: SharedData, block: Block) {
        val mapName = block.getString(ConfigKey.LocalMapBlock.key, "data")
        val localMap = sharedData.getLocalMap<String, String>(mapName)
        val configuredMap = block.asStringMap() - ConfigKey.LocalMapBlock.key
        localMap.putAll(configuredMap)
        log.debug("Uploaded data to local map [{}]: {}", mapName, configuredMap)
    }

    companion object {
        val log: KLogger = KotlinLogging.logger {}
    }
}
