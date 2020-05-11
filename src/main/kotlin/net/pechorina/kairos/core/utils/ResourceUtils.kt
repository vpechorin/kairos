package net.pechorina.kairos.core.utils

import net.pechorina.kairos.core.verticles.MainVerticle
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.regex.Pattern

object ResourceUtils {
    fun getBootResources(): List<String> {
        val reflections = Reflections(ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("boot/"))
                .setScanners(ResourcesScanner()))
        val resources = reflections.getResources(Pattern.compile(".*\\.yml"))
        return resources.filter { it.startsWith(MainVerticle.BOOT_RESOURCES) }
    }
}