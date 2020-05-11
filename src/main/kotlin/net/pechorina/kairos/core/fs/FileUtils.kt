package net.pechorina.kairos.core.fs

import com.google.common.io.CharStreams

import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {

    fun byteChannelRead(file: File): String {
        val filePath = Paths.get(file.toURI())
        val builder = StringBuilder()
        Files.newByteChannel(filePath).use { byteChannel ->
            val byteBuffer = ByteBuffer.allocate(512)
            val charset = StandardCharsets.UTF_8
            while (byteChannel.read(byteBuffer) > 0) {
                byteBuffer.rewind()
                builder.append(charset.decode(byteBuffer))
                byteBuffer.flip()
            }
        }

        return builder.toString()
    }

    fun readFile(file: File): String {
        return CharStreams.toString(FileReader(file))
    }
}
