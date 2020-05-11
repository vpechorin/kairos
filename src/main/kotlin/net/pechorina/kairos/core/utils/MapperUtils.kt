package net.pechorina.kairos.core.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator

object YamlMapper {

    val yamlObjectMapper: ObjectMapper

    init {
        yamlObjectMapper = ObjectMapper(
                YAMLFactory()
                        .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                        .configure(YAMLGenerator.Feature.INDENT_ARRAYS, true)
                        .enable(JsonParser.Feature.ALLOW_YAML_COMMENTS)
                        .enable(JsonParser.Feature.ALLOW_MISSING_VALUES)
        ).setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .findAndRegisterModules()
    }
}

object JsonMapper {
    val jsonObjectMapper = ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

object XmlObjectMapper {
    val xmlObjectMapper = XmlMapper()
}