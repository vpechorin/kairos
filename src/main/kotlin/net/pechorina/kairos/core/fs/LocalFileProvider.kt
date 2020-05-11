package net.pechorina.kairos.core.fs


import net.pechorina.kairos.core.serialization.Deserializer
import net.pechorina.kairos.core.serialization.Serializer
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path

class LocalFileProvider(val serializer: Serializer? = null, val deserializer: Deserializer? = null) : FileProvider {

    override fun save(entity: Any, path: Path) {
        Files.createDirectories(path.getParent());

        try {
            FileOutputStream(path.toFile()).use { output -> serializer?.serialize(entity, output) }
        } catch (e: IOException) {
            throw UncheckedIOException(String.format("Can't write data to file [%s]", path), e)
        }
    }

    override fun load(path: Path): Any {
        deserializer ?: throw RuntimeException("Deserializer is not defined")
        try {
            FileInputStream(path.toFile())
                    .use { fileInputStream ->
                        return deserializer.deserialize(fileInputStream)
                    }
        } catch (e: IOException) {
            throw UncheckedIOException(String.format("Can't read file [%s]", path), e)
        } catch (uio: UncheckedIOException) {
            throw UncheckedIOException(String.format("Can't read file [%s]", path), uio.cause)
        }
    }

    override fun delete(path: Path) {
        Files.deleteIfExists(path)
    }
}
