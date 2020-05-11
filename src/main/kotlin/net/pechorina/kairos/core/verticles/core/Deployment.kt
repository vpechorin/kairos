package net.pechorina.kairos.core.verticles.core

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import mu.KotlinLogging
import net.pechorina.kairos.core.DeploymentType
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.verticles.DEPLOY_ADDRESS

class Deployment(val vertx: Vertx, val config: JsonObject) {

    fun deploy(stageDefinition: StageDefinition): Future<String> {
        return deploy(stageDefinition, stageDefinition.deploymentType)
    }

    fun deploy(stageDefinition: StageDefinition, deploymentType: DeploymentType): Future<String> {
        log.debug { "Deploying ${stageDefinition.type}  ${stageDefinition.instanceName}" }
        if (isBlacklisted(stageDefinition)) {
            val message = "Blacklisted: ${stageDefinition.instanceName}/${stageDefinition.type}"
            log.info { message }
            return Future.failedFuture(message)
        }

        val stageConfig = JsonObject()
        stageConfig.put("definition", JsonObject.mapFrom(stageDefinition))
        stageConfig.put("config", config)

        val deploymentOptions = DeploymentOptions()
                .setConfig(stageConfig)
                .setHa(stageDefinition.highAvailability)
                .setInstances(stageDefinition.instances)
                .setWorker(stageDefinition.worker)
                .setMaxWorkerExecuteTime(300_000_000L)

        val promise = Promise.promise<String>()

        when (deploymentType) {
            DeploymentType.LOCAL -> vertx.deployVerticle(stageDefinition.type, deploymentOptions, promise)
            DeploymentType.SINGLE -> {
                vertx.eventBus().send(DEPLOY_ADDRESS, stageDefinition);
                promise.complete("to deploy remotely")
            }
            DeploymentType.GLOBAL -> {
                vertx.eventBus().publish(DEPLOY_ADDRESS, stageDefinition)
                promise.complete("to deploy globally")
            }
        }

        return promise.future()
    }

    fun isBlacklisted(stage: StageDefinition): Boolean {
        stage.instanceName
        val blacklist = config.getJsonArray("blacklist", JsonArray())
        return blacklist.any {
            val regex = it.toString().toRegex()
            return regex.matches(stage.instanceName) || regex.matches(stage.type)
        }
    }

    companion object {
        val log = KotlinLogging.logger {}
    }
}