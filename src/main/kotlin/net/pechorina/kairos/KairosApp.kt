package net.pechorina.kairos

import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import mu.KotlinLogging
import net.pechorina.kairos.core.loadConfig

private val log = KotlinLogging.logger {}

object KairosApp {

    init {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
        System.setProperty("hazelcast.logging.type", "log4j2")
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        log.debug("START")
        val vertx = Vertx.vertx() // Use a temporary instance of Vertx to load config
        loadConfig(vertx).onSuccess { config ->
            log.debug { "Configuration: ${config.encodePrettily()}" }
            // Close the temp vert.x instance, we don't need it anymore.
            vertx.close {
                start(config)
            }
        }
    }

    @JvmStatic
    private fun start(config: JsonObject) {
        val vertxOptions = VertxOptions()

        val poolSize = config.getInteger("workerPoolSize")
        if (poolSize != null) vertxOptions.setWorkerPoolSize(poolSize)

        if (config.getBoolean("clustered", false)) {
            Vertx.clusteredVertx(vertxOptions) {
                if (it.succeeded()) {
                    log.error("Vertx(clustered) is starting...")
                    val deploymentOptions = DeploymentOptions().setConfig(config)
                    it.result()?.deployVerticle(
                            "net.pechorina.kairos.core.verticles.MainVerticle",
                            deploymentOptions
                    )
                } else {
                    log.error("Vertx startup failed", it.cause())
                }
            }
        } else {
            val vertx = Vertx.vertx(vertxOptions)
            log.error("Vertx(non clustered) is starting...")
            val deploymentOptions = DeploymentOptions().setConfig(config)
            vertx.deployVerticle(
                    "net.pechorina.kairos.core.verticles.MainVerticle",
                    deploymentOptions
            )
        }
    }
}
