package net.pechorina.kairos.core.verticles

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import mu.KotlinLogging
import net.pechorina.kairos.core.DeploymentType
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.events.KEvent
import net.pechorina.kairos.core.types.Status
import net.pechorina.kairos.core.verticles.core.Deployment
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

val DEPLOY_ADDRESS = "deploy.verticle"

class DeployerVerticle : AbstractVerticle() {

    var deployments: MutableMap<String, StageDefinition> = ConcurrentHashMap()
    var blacklist: List<String> = emptyList()
    lateinit var deployment: Deployment

    override fun start(startPromise: Promise<Void>) {
        deployment = Deployment(vertx, config())

        blacklist = config()
                .getJsonArray("blacklist", JsonArray())
                .map { it.toString() }

        vertx.eventBus()
                .consumer(DEPLOY_ADDRESS, this::handle)
                .completionHandler {
                    log.debug("Verticle {} deployed", DeployerVerticle::class.java.simpleName)
                    startPromise.complete()
                }
    }

    private fun handle(message: Message<StageDefinition>) {
        val stageDefinition = message.body()
        if (stageDefinition == null) {
            log.error("No stage definition receive")
        }

        deployment.deploy(stageDefinition, DeploymentType.LOCAL)
                .onSuccess { deployments[it] = stageDefinition }
                .onFailure { message.reply(KEvent(it.message ?: "Error").setStatus(Status.INTERNAL_SERVER_ERROR)) }

    }

    companion object {
        val log = KotlinLogging.logger {}
    }
}
