package org.stt

import org.stt.gui.UIMain
import java.io.File
import java.net.URL
import java.net.URLClassLoader

object StartWithJFX {

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            Class.forName("javafx.embed.swing.JFXPanel")
        } catch (e: ClassNotFoundException) {
            val jfxrt = retrieveJFXRTFile()
            val systemClassLoader = ClassLoader
                    .getSystemClassLoader() as URLClassLoader
            addUrlToURLClassLoader(jfxrt, systemClassLoader)
        }

        UIMain.main(args)
    }

    private fun addUrlToURLClassLoader(jfxrt: File,
                                       systemClassLoader: URLClassLoader) {
        val addUrlMethod = URLClassLoader::class.java.getDeclaredMethod("addURL",
                URL::class.java)
        addUrlMethod.isAccessible = true
        addUrlMethod.invoke(systemClassLoader, jfxrt.toURI().toURL())
        addUrlMethod.isAccessible = false
    }

    private fun retrieveJFXRTFile(): File {
        val jfxrt = File(System.getProperty("java.home") + "/lib/jfxrt.jar")
        if (!jfxrt.exists()) {
            throw IllegalStateException("Couldn't locate $jfxrt")
        }
        return jfxrt
    }
}
