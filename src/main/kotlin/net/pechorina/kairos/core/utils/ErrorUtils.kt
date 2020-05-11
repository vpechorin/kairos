package net.pechorina.kairos.core.utils

import java.io.PrintWriter
import java.io.StringWriter

fun stackTraceString(throwable: Throwable?): String {
    if (throwable == null) return "No error"
    val stringWriter = StringWriter()
    throwable.printStackTrace(PrintWriter(stringWriter))
    return stringWriter.toString()
}