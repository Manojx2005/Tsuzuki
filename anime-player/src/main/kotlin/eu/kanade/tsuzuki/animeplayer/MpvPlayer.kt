package eu.kanade.tsuzuki.animeplayer

import android.content.Context
import android.view.Surface
import `is`.xyz.mpv.MPVLib

/**
 * Thin Kotlin helpers over the MIT [MPVLib] JNI wrapper. Loading the native libraries happens when MPVLib's static
 * initialiser runs, so [isAvailable] must be checked before any other call on devices without arm64 libraries.
 */
object MpvPlayer {

    /** True when libmpv and libplayer could be loaded on this device (arm64-v8a only). */
    val isAvailable: Boolean by lazy {
        try {
            Class.forName("is.xyz.mpv.MPVLib")
            true
        } catch (_: Throwable) {
            // ExceptionInInitializerError wraps UnsatisfiedLinkError when the ABI has no libraries.
            false
        }
    }

    fun create(context: Context, logLevel: String = "warn") = MPVLib.create(context.applicationContext, logLevel)

    fun option(name: String, value: String) {
        check(MPVLib.setOptionString(name, value) >= 0) { "mpv rejected option: $name" }
    }

    fun init() = MPVLib.init()

    fun attachSurface(surface: Surface) = MPVLib.attachSurface(surface)

    fun detachSurface() = MPVLib.detachSurface()

    fun command(vararg args: String) = MPVLib.command(arrayOf(*args))

    fun observe(property: String, format: Int, observer: MPVLib.EventObserver) {
        MPVLib.addObserver(observer)
        MPVLib.observeProperty(property, format)
    }

    fun removeObserver(observer: MPVLib.EventObserver) = MPVLib.removeObserver(observer)

    fun destroy() = MPVLib.destroy()
}
