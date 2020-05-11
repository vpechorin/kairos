package net.pechorina.kairos.utils

import net.pechorina.kairos.core.utils.TemplateEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TemplateEngineUnitTest {

    @Test
    fun simpleRednerCheck() {
        val templateContext = mapOf(
                "env" to "DEV"
        )

        assertThat(TemplateEngine.process("{{ env }}", templateContext)).isEqualTo("DEV")
    }
}