package net.pechorina.kairos.core

data class Section(var name: String = DEFAULT_SECTION_NAME, var blocks: List<Block> = arrayListOf()) {

    fun block(index: Int = 0): Block {
        return if (blocks.isEmpty()) Block() else blocks[index]
    }

    fun block(key: String, value: Any): Block {
        return blocks.find { it.matches(key, value) } ?: Block()
    }

    companion object {
        val DEFAULT_SECTION_NAME = "main"
    }
}
