package ca.exp.soundboard.rewrite.soundboard

import ca.exp.soundboard.rewrite.soundboard.KeyEventIntConverter.getKeyEventText
import com.google.gson.Gson
import org.jnativehook.keyboard.NativeKeyEvent
import java.io.*
import java.util.*

class Soundboard {
    val soundboardEntries: ArrayList<SoundboardEntry> = ArrayList()
    val entriesAsObjectArrayForTable: Array<Array<Any?>>
        get() {
            val array = Array(soundboardEntries.size) { arrayOfNulls<Any>(4) }

            for (i in array.indices) {
                val entry = soundboardEntries[i]
                array[i][0] = entry.fileName
                array[i][1] = entry.activationKeysAsReadableString
                array[i][2] = entry.fileString
                array[i][3] = Integer.valueOf(i)
            }

            return array
        }

    fun addEntry(file: File, keyNumbers: IntArray?) {
        soundboardEntries.add(SoundboardEntry(file, keyNumbers))
    }

    fun getEntry(filename: String): SoundboardEntry? {
        for (entry in soundboardEntries) {
            if (entry.fileName == filename) {
                return entry
            }
        }

        return null
    }

    fun removeEntry(index: Int) {
        soundboardEntries.removeAt(index)
    }

    fun removeEntry(filename: String) {
        for (entry in soundboardEntries) {
            if (entry.fileName == filename) {
                soundboardEntries.remove(entry)

                break
            }
        }
    }

    fun saveAsJsonFile(file: File): File {
        var filestring = file.absolutePath
        println(filestring)

        if (filestring.contains(".")) {
            filestring = filestring.substring(0, filestring.lastIndexOf('.'))
        }

        filestring = "$filestring.json"
        println("amended: $filestring")

        val gson = Gson()
        val json = gson.toJson(this)
        val realFile = File(filestring)
        val writer: BufferedWriter?

        try {
            writer = BufferedWriter(FileWriter(realFile))
            writer.write(json)
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return realFile
    }

    fun getEntry(index: Int): SoundboardEntry? {
        try {
            return soundboardEntries[index]
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return null
    }

    fun entriesContainPTTKeys(pttkeys: List<Int>): Boolean {
        if (pttkeys != pttKeysClone || hasSoundboardChanged()) {
            soundboardEntriesClone = soundboardEntries
            pttKeysClone = pttkeys as ArrayList<Int>
            var key: String?

            for ((i, entry) in soundboardEntries.withIndex()) {
                var arrayOfInt: IntArray
                entry.activationKeys.also { arrayOfInt = it!! }!!.size
                val actKey = arrayOfInt[i]
                key = NativeKeyEvent.getKeyText(actKey).lowercase(Locale.getDefault())
                for (number in pttkeys) {
                    if (key == getKeyEventText(number).lowercase(Locale.getDefault())) {
                        containsPPTKey = true

                        return true
                    }
                }
            }

            containsPPTKey = false

            return false
        }

        return containsPPTKey
    }

    private fun hasSoundboardChanged(): Boolean {
        if (soundboardEntries != soundboardEntriesClone) {
            println("SoundboardStage changed")
            return true
        }

        return false
    }

    companion object {
        private var containsPPTKey = false
        private var pttKeysClone = ArrayList<Int>()
        private var soundboardEntriesClone = ArrayList<SoundboardEntry>()

        fun loadFromJsonFile(file: File): Soundboard {
            var br: BufferedReader? = null

            try {
                br = BufferedReader(FileReader(file))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            val json = Gson()
            val sb = json.fromJson(br, Soundboard::class.java)

            try {
                br!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return sb
        }
    }
}
