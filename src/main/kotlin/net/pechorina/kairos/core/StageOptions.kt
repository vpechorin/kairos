package net.pechorina.kairos.core

import mu.KotlinLogging
import net.pechorina.kairos.core.utils.DataTypeUtils
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.streams.toList

class StageOptions(val options: List<Map<String, String>> = arrayListOf()) {

    private val opts: MutableMap<String, Section>

    init {
        this.opts = LinkedHashMap()
        val groupedBySection = options.groupBy { toSectionName(it) }
        for (section in groupedBySection.keys) {
            val blockMaps = groupedBySection.getOrDefault(section, emptyList())

            val blocks = blockMaps.map { processBlock(it) }
            val optionSection = Section(section, blocks)
            this.opts[section] = optionSection
        }
    }

    fun section(sectionName: String): Section {
        val section = opts[sectionName.toLowerCase()]
        if (section == null) {
            log.warn("Section '{}' not found", sectionName)
        }
        return section ?: Section()
    }

    fun section(sectionName: ConfigKey): Section {
        return section(sectionName.key)
    }

    fun firstSection(): Section {
        if (opts.isEmpty()) Section()
        val s: Section? = opts.values.firstOrNull()
        return s ?: Section()
    }

    private fun processBlock(block: Map<String, String>): Block {
        val out: MutableMap<String, Any> = linkedMapOf()
        for (key in block.keys) {
            if (key.equals("section", ignoreCase = true)) continue
            val stringValue = block[key] ?: continue
            val typeValue = DataTypeUtils.getTypeAndConvert(stringValue)
            if (log.isTraceEnabled) log.trace("{}({}): {}", key, typeValue.first, typeValue.second)
            typeValue.second?.let { out[key] = it }
        }
        return Block(out)
    }

    private fun toSectionName(map: Map<String, String>): String {
        val sectionsNames = map.keys.stream()
                .filter { key -> key.equals("section", ignoreCase = true) }
                .map { key -> map.getOrDefault(key, Section.DEFAULT_SECTION_NAME) }
                .distinct()
                .toList()

        if (sectionsNames.size > 1) {
            throw IllegalStateException("Multiple section names in the configuration block ${map}")
        }
        return if (sectionsNames.size == 1) sectionsNames[0].toLowerCase() else Section.DEFAULT_SECTION_NAME
    }

    companion object {
        val log = KotlinLogging.logger {}
    }
}
