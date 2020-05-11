package net.pechorina.kairos.core.utils

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.error.PebbleException
import com.mitchellbosecke.pebble.loader.StringLoader
import com.mitchellbosecke.pebble.template.PebbleTemplate
import mu.KotlinLogging
import java.io.IOException
import java.io.StringWriter
import java.io.Writer


object TemplateEngine {
    var engine = PebbleEngine.Builder()
            .loader(StringLoader())
            .autoEscaping(false)
            .strictVariables(false)
            .newLineTrimming(false)
            .build()

    val log = KotlinLogging.logger {}

    fun process(template: String, context: Map<String, Any?>): String {
        val writer: Writer = StringWriter()

        val compiledTemplate: PebbleTemplate
        try {
            compiledTemplate = engine.getTemplate(template)
            compiledTemplate.evaluate(writer, context)
        } catch (e: PebbleException) {
            log.error("Error compiling template $template", e)
            throw RuntimeException("Error compiling template $template", e)
        } catch (e: IOException) {
            log.error("Error compiling template $template", e)
            throw RuntimeException("Error compiling template $template", e)
        }

        writer.flush()

        return writer.toString()
    }
}