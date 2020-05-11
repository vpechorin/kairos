package net.pechorina.kairos.core.mappers

import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import groovy.text.TemplateEngine
import java.io.StringWriter

class GroovyStringMapper(val templateText: String?) {

    private val engine: TemplateEngine
    private val template: Template

    init {
        this.engine = StreamingTemplateEngine()
        this.template = engine.createTemplate(templateText)
    }

    fun map(input: Any): String {
        val parameters = mapOf("input" to input)
        try {
            val result = template.make(parameters)
            val writer = StringWriter()

            writer.use {
                result.writeTo(writer)
                return writer.toString()
            }

        } catch (e: RuntimeException) {
            throw RuntimeException("Error processong template", e)
        }
    }
}
