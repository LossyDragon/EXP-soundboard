package ca.exp.soundboard.rewrite.soundboard

import org.jnativehook.keyboard.NativeKeyEvent
import java.io.File
import java.io.UnsupportedEncodingException
import java.nio.file.Paths

class SoundboardEntry(file: File, keys: IntArray?) {

    var activationKeys: IntArray?

    var fileString: String
        private set

    val fileName: String
        get() = fileString.substring(fileString.lastIndexOf(File.separatorChar) + 1)

    val activationKeysAsReadableString: String
        get() {
            var s = ""
            if (activationKeys!!.isEmpty())
                return s

            var arrayOfInt: IntArray
            val j = activationKeys.also { arrayOfInt = it!! }!!.size

            for (i in 0 until j) {
                val i2 = arrayOfInt[i]
                s = s + NativeKeyEvent.getKeyText(i2) + "+"
            }

            s = s.substring(0, s.length - 1)

            return s
        }

    init {
        val p = Paths.get(file.absolutePath)

        fileString = p.toAbsolutePath().toString()
        activationKeys = keys

        if (activationKeys == null)
            activationKeys = IntArray(0)
    }

    fun matchesPressed(pressedKeys: ArrayList<Int>): Boolean {
        var keysRemaining = activationKeys!!.size

        if (keysRemaining == 0)
            return false

        var arrayOfInt: IntArray
        val j = activationKeys.also { arrayOfInt = it!! }!!.size

        for (i in 0 until j) {
            val actkey = arrayOfInt[i]
            val localIterator: Iterator<*> = pressedKeys.iterator()

            while (localIterator.hasNext()) {
                val presskey = (localIterator.next() as Int).toInt()

                if (actkey == presskey) {
                    keysRemaining--
                }
            }
        }

        return keysRemaining <= 0
    }

    fun matchesHowManyPressed(pressedKeys: ArrayList<Int>): Int {
        var matches = 0

        for (i in pressedKeys.indices) {
            var arrayOfInt: IntArray
            val len = activationKeys.also { arrayOfInt = it!! }!!.size
            val key = pressedKeys[i]

            if (i < len) {
                val hotkey = arrayOfInt[i]
                if (key == hotkey) {
                    matches++
                }
            }
        }

        return matches
    }

    fun play(audio: AudioManager, moddedspeed: Boolean) {
        val file = toFile()
        audio.playSoundClip(file, moddedspeed)
    }

    private fun toFile(): File {
        val f = File(fileString)

        if (!f.exists()) {
            val p = Paths.get(fileString)
            return p.toFile()
        }

        return f
    }

    fun setFile(file: File) {
        try {
            fileString = file.absolutePath.toByteArray(charset(Utils.fileEncoding)).toString()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }
}
