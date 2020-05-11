package net.pechorina.kairos.core

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

fun loadConfig(vertx: Vertx): Future<JsonObject> {

    val localStore = ConfigStoreOptions()
            .setOptional(true)
            .setType("file")
            .setFormat("yaml")
            .setConfig(JsonObject().put("path", "config.yml"))

    val sysPropsStore = ConfigStoreOptions()
            .setOptional(true)
            .setType("sys")

    val jsonEnvStore = ConfigStoreOptions().setType("env").setOptional(true)

    val configRetrieverOptions = ConfigRetrieverOptions()
            .addStore(localStore)
            .addStore(sysPropsStore)
            .addStore(jsonEnvStore)


    val retriever = ConfigRetriever.create(vertx, configRetrieverOptions)

    val promise = Promise.promise<JsonObject>()
    retriever.getConfig(promise)
    return promise.future();
}