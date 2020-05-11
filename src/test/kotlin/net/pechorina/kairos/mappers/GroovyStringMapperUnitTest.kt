package net.pechorina.kairos.mappers

import net.pechorina.kairos.core.mappers.GroovyStringMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GroovyStringMapperUnitTest {

    @Test
    fun stringTrimTest() {
        val mapper = GroovyStringMapper("\${input.trim()}")
        val result = mapper.map("  aaa   ")
        assertThat(result).isEqualTo("aaa")
    }

    @Test
    fun jsonGenTest() {
        val mapper = GroovyStringMapper("{'aaa': '\${input}'}")
        val result = mapper.map("vvv")
        assertThat(result).isEqualTo("{'aaa': 'vvv'}")
    }

}