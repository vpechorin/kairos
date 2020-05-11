package net.pechorina.kairos.core.verticles

import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import mu.KotlinLogging
import net.pechorina.kairos.core.StageDefinition
import net.pechorina.kairos.core.utils.ClassUtils
import net.pechorina.kairos.core.utils.ResourceUtils
import net.pechorina.kairos.core.verticles.core.Deployment
import net.pechorina.kairos.core.verticles.core.registerMessageCodecs
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.nio.file.Paths

class MainVerticle : AbstractVerticle() {
    companion object {
        val log = KotlinLogging.logger {}
        val BOOT_RESOURCES = "boot"
    }

    lateinit var deployment: Deployment

    override fun start(startFuture: Future<Void>?) {
        log.info { "Starting application - MainVerticle" }

        val config = config()

        deployment = Deployment(vertx, config())

        startApp(config)
                .compose { config -> boot(config) }
                .compose { config -> loadUserStages(config) }
                .onSuccess { startFuture?.complete() }
                .onFailure { e -> startFuture?.fail(e.cause) }
    }

    fun startApp(config: JsonObject): Future<JsonObject> {
        registerMessageCodecs(vertx)

        log.debug("Deploying \'deployer\' verticle")
        val options = DeploymentOptions().setConfig(config)
        val deploymentPromise = Promise.promise<String>()
        val startPromise = Promise.promise<JsonObject>()
        vertx.deployVerticle("net.pechorina.kairos.core.verticles.DeployerVerticle", options, deploymentPromise)

        deploymentPromise.future()
                .onSuccess { _ -> startPromise.complete(config) }
                .onFailure { e -> startPromise.fail(e) }

        return startPromise.future()
    }

    fun loadStage(resource: String, config: JsonObject): StageDefinition {
        val classLoader = ClassUtils.defaultClassLoader
        try {
            classLoader?.getResourceAsStream(resource)
                    .use { inputStream -> return StageDefinition.fromYamlInputStream(resource, inputStream, config) }
        } catch (e: IOException) {
            log.error("Can't read stage $resource", e)
            throw UncheckedIOException("Can't read stage $resource", e)
        }
    }

    private fun boot(config: JsonObject): Future<JsonObject> {
        val noBoot = config.getBoolean("noboot", false)
        if (noBoot) {
            log.info { "NoBoot parameter - skip deploying core verticles" }
            return Future.succeededFuture(config)
        }

        log.debug("Deploying core verticles")
        val futures = ResourceUtils.getBootResources()
                .map { stage ->
                    try {
                        return@map loadStage(stage, config)
                    } catch (e: Throwable) {
                        log.error("Failed to deserialize the stage", e)
                    }
                    return@map null
                }
                .filterNotNull()
                .map { deployment.deploy(it) }

        val promise = Promise.promise<JsonObject>()
        CompositeFuture.join(futures)
                .onSuccess { promise.tryComplete(config) }
                .onFailure { log.warn("Startup error", it); promise.tryComplete(config) }

        return promise.future()
    }

    private fun loadUserStages(config: JsonObject): Future<Void> {
        val rootPath = Paths.get(config.getString("userStages", "./stages"))

        val futures = loadStages(rootPath)
                .mapNotNull { file -> StageDefinition.fromFile(file.name, file, config) }
                .map { deployment.deploy(it) }

        val promise = Promise.promise<Void>()
        CompositeFuture.join(futures)
                .onSuccess { promise.tryComplete() }
                .onFailure { log.warn("Error loading user stages", it); promise.tryComplete() }

        return promise.future()
    }

    private fun loadStages(path: Path): List<File> {
        val yamlFiles: (File) -> Boolean = { f -> f.isFile && f.canRead() && f.name.endsWith(".yml", ignoreCase = true) }
        return path.toFile().walkTopDown().maxDepth(9).filter(yamlFiles).toList()
    }
}
