package net.pechorina.kairos.utils

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.verticles.DEPLOY_ADDRESS

fun deployStage(stageDefinition: StageDefinition, vertx: Vertx, testContext: VertxTestContext, commonConfig: JsonObject = JsonObject()) {

    val config = JsonObject()
    config.put("definition", JsonObject.mapFrom(stageDefinition))
    config.put("config", commonConfig)
    config.put("noboot", true)

    val deploymentOptions = DeploymentOptions()
            .setConfig(config)
            .setHa(stageDefinition.highAvailability)
            .setInstances(stageDefinition.instances)
            .setWorker(stageDefinition.worker)
            .setMaxWorkerExecuteTime(300_000_000L)

    vertx.deployVerticle(stageDefinition.type, deploymentOptions) {
        if (it.failed()) {
            log.error { "${stageDefinition.type}/${stageDefinition.instanceName} - ERROR(${it.cause()})" }
            testContext.failNow(it.cause())
        } else {
            log.error { "${stageDefinition.type}/${stageDefinition.instanceName} - OK" }
            testContext.completeNow()
        }
    }
}

fun deployStages(stageDefinitions: List<StageDefinition>, vertx: Vertx, testContext: VertxTestContext) {
    val config = JsonObject().put("noboot", true)
    val deploymentOptions = DeploymentOptions().setConfig(config)

    vertx.deployVerticle("net.pechorina.kairos.core.verticles.MainVerticle", deploymentOptions) {
        GlobalScope.launch(vertx.dispatcher()) {
            stageDefinitions.forEach { stageDefinition ->
                awaitResult<Message<KEvent>> { handler ->
                    vertx.eventBus().request(DEPLOY_ADDRESS, stageDefinition, handler)
                }

            }
            testContext.completeNow()
        }
    }
}

val log: KLogger = KotlinLogging.logger {}


