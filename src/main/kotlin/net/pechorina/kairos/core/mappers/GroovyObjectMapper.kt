package net.pechorina.kairos.core.mappers

import groovy.lang.GroovyShell

class GroovyObjectMapper {

    private val shell: GroovyShell = GroovyShell()

    fun map(expression: String?, input: Any?, context: Any? = null): Any {
        shell.removeVariable("input")
        shell.removeVariable("context")
        try {
            shell.setVariable("input", input)
            context?.let { shell.setVariable("context", context) }
            return shell.evaluate(expression)
        } catch (e: RuntimeException) {
            throw RuntimeException(String.format("Error using expression: [%s]", expression), e)
        }
    }

}
