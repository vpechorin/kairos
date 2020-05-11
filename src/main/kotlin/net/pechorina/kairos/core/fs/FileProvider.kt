package net.pechorina.kairos.core.fs

import java.nio.file.Path

interface FileProvider {
    fun save(entity: Any, path: Path)

    fun load(path: Path): Any

    fun delete(path: Path)
}
