package net.pechorina.kairos.core.utils

object ClassUtils {
    // Cannot access thread context ClassLoader - falling back...
    // No thread context class loader -> use class loader of this class.
    // getClassLoader() returning null indicates the bootstrap ClassLoader
    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
    val defaultClassLoader: ClassLoader?
        get() {
            var cl: ClassLoader? = null
            try {
                cl = Thread.currentThread().contextClassLoader
            } catch (ex: Throwable) {
            }

            if (cl == null) {
                cl = ClassUtils::class.java.classLoader
                if (cl == null) {
                    try {
                        cl = ClassLoader.getSystemClassLoader()
                    } catch (ex: Throwable) {
                    }

                }
            }
            return cl
        }
}
