package net.pechorina.kairos.core

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.STRING)
enum class DeploymentType {
    LOCAL,
    SINGLE,
    GLOBAL
}