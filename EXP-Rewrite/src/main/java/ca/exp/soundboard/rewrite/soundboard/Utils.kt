package ca.exp.soundboard.rewrite.soundboard

import ca.exp.soundboard.rewrite.gui.SettingsFrame
import ca.exp.soundboard.rewrite.gui.SoundboardFrame
import ca.exp.soundboard.rewrite.soundboard.KeyEventIntConverter.getKeyEventText
import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.keyboard.NativeKeyEvent
import java.awt.AWTException
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences
import javax.sound.sampled.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object Utils {

    private const val BUFFERSIZE = 2048
    private const val STANDARDSAMPLERATE = 44100.0f
    private const val modifiedSpeedIncrements = 0.05f
    private const val modifiedSpeedMax = 2.0f
    private const val modifiedSpeedMin = 0.1f
    private const val prefsName = "ca/exp/soundboard" // HKEY_CURRENT_USER\SOFTWARE\JavaSoft

    private val clipPlayerThreadGroup = ThreadGroup("Clip Player Group")
    private val lastNativeKeyPressMap = ConcurrentHashMap<String, Long>()
    private val lastRobotKeyPressMap = ConcurrentHashMap<String, Long>()
    private var PLAYALL = true
    private var currentlyPlayingClipCount = 0
    private var microphoneInjector = MicInjector()
    private var modifiedPlaybackSpeed = 0f
    private var pttkeys = listOf<Int>()
    private var robot: Robot? = null

    val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, STANDARDSAMPLERATE, 16, 2, 4, STANDARDSAMPLERATE, false)
    val prefs: Preferences = Preferences.userRoot().node(prefsName)
    var fileEncoding: String = System.getProperty("file.encoding")
    var isAutoPTThold = true
    var modifiedPlaybackFormat: AudioFormat? = null
    var modifiedSpeedKey = 35
    var modspeeddownKey = 37
    var modspeedupKey = 39
    var overlapSwitchKey = 36
    var stopKey = 19

    private val isMicInjectorRunning: Boolean
        get() = micInjector.isRunning

    private val micInjector: MicInjector
        get() = microphoneInjector

    var micInjectorGain: Float
        get() = MicInjector.gain
        set(level) {
            micInjector.setGain(level)
        }

    var overlapSameClipWhilePlaying = true
        set(value) {
            if (SettingsFrame.instance != null) {
                SettingsFrame.instance?.updateOverlapSwitchCheckBox()
            }
            field = value
        }

    var pTTkeys: List<Int>
        get() = pttkeys
        set(value) {
            pttkeys = value
        }

    fun getFileChooser(): JFileChooser {
        return SoundboardFrame.filechooser
    }

    private val robotInstance: Robot?
        get() {
            if (robot != null) {
                return robot
            }

            try {
                robot = Robot()
            } catch (e: AWTException) {
                e.printStackTrace()
            }

            return if (robot != null) robot else null
        }

    fun playNewSoundClipThreaded(file: File, primarySpeaker: SourceDataLine?, secondarySpeaker: SourceDataLine?) {
        SwingUtilities.invokeLater {
            val clip = ClipPlayer(file, primarySpeaker, secondarySpeaker)

            if (!overlapSameClipWhilePlaying) {
                stopFilePlaying(file)
            }

            clip.start()
        }
    }

    fun getMixerNames(lineInfo: DataLine.Info?): Array<String?> {
        val info = AudioSystem.getMixerInfo()
        var arrayOfInfo1: Array<Mixer.Info>
        val j = info.also { arrayOfInfo1 = it }.size
        val mixerNames = ArrayList<String>()

        for (i in 0 until j) {
            val elem = arrayOfInfo1[i]
            val mixer = AudioSystem.getMixer(elem)

            try {
                if (mixer.isLineSupported(lineInfo)) {
                    mixerNames.add(elem.name)
                }
            } catch (e: NullPointerException) {
                System.err.println(e)
            }
        }

        val returnArray = arrayOfNulls<String>(mixerNames.size)

        return mixerNames.toArray(returnArray)
    }

    fun stopAllClips() {
        PLAYALL = false
        zeroCurrentClipCount()
    }

    fun startMicInjector(inputMixerName: String, outputMixerName: String) {
        var inputexists = false
        var outputexists = false

        if (isMicInjectorRunning)
            stopMicInjector()

        var arrayOfString: Array<String?>
        var j = MicInjector.getMixerNames(MicInjector.targetDataLineInfo).also { arrayOfString = it }.size

        for (i in 0 until j) {
            val mixer = arrayOfString[i]
            if (mixer == inputMixerName) {
                inputexists = true
            }
        }

        j = MicInjector.getMixerNames(MicInjector.sourceDataLineInfo).also { arrayOfString = it }.size

        for (i in 0 until j) {
            val mixer = arrayOfString[i]

            if (mixer == outputMixerName) {
                outputexists = true
            }
        }

        if (inputexists && outputexists) {
            micInjector.setInputMixer(inputMixerName)
            micInjector.setOutputMixer(outputMixerName)
            micInjector.start()
        }
    }

    fun stopMicInjector() {
        micInjector.stopRunning()
    }

    fun initGlobalKeyLibrary(): Boolean {
        try {
            GlobalScreen.registerNativeHook()
        } catch (ex: NativeHookException) {
            System.err.println("There was a problem registering the native hook.")
            System.err.println(ex.message)

            JOptionPane.showMessageDialog(
                null, "Error: " + ex.message,
                "Error occured whilst initiating global hotkeys", 0
            )
        }

        return true
    }

    fun deregisterGlobalKeyLibrary(): Boolean {
        if (GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.unregisterNativeHook()
            } catch (e: NativeHookException) {
                e.printStackTrace()
            }

            return true
        }

        return false
    }

    fun isFileSupported(file: File?): Boolean {
        try {
            AudioSystem.getAudioFileFormat(file)

            return true
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    @Synchronized
    fun getModifiedPlaybackSpeed(): Float {
        return modifiedPlaybackSpeed
    }

    @Synchronized
    fun setModifiedPlaybackSpeed(speed: Float) {
        modifiedPlaybackSpeed = speed
        val newSampleRate = STANDARDSAMPLERATE * speed
        modifiedPlaybackFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, newSampleRate, 16, 2, 4,
            newSampleRate, false
        )
    }

    fun incrementModSpeedUp() {
        var speed = modifiedPlaybackSpeed + modifiedSpeedIncrements
        if (speed > modifiedSpeedMax) {
            speed = modifiedSpeedMax
        }
        setModifiedPlaybackSpeed(speed)
        if (SettingsFrame.instance != null) {
            SettingsFrame.instance?.updateDisplayedModSpeed()
        }
    }

    fun decrementModSpeedDown() {
        var speed = modifiedPlaybackSpeed - modifiedSpeedIncrements
        if (speed < modifiedSpeedMin) {
            speed = modifiedSpeedMin
        }
        setModifiedPlaybackSpeed(speed)
        if (SettingsFrame.instance != null) {
            SettingsFrame.instance?.updateDisplayedModSpeed()
        }
    }

    fun checkAndUseAutoPPThold(): Boolean {
        if (!isAutoPTThold || pttkeys.isEmpty()) {
            return false
        }

        if (SoundboardFrame.soundboard.entriesContainPTTKeys(pttkeys)) {
            return false
        }

        val pressed = SoundboardFrame.macroListener.pressedNativeKeys
        val robot = robotInstance
        val noofkeys = pttkeys.size

        for (i in 0 until noofkeys) {
            val key = pttkeys[i]
            var pressedAlready = false

            for (nativekey in pressed) {
                if (getKeyEventText(key).lowercase(Locale.getDefault()) ==
                    NativeKeyEvent.getKeyText(nativekey!!).lowercase(Locale.getDefault())
                ) {
                    pressedAlready = true

                    break
                }
            }

            if (!pressedAlready) {
                robot?.keyPress(key)

                submitRobotKeyPressTime(getKeyEventText(key))

                println("Robot pressed: " + KeyEvent.getKeyText(key))
            }
        }

        return true
    }

    fun checkAndReleaseHeldPPTKeys(): Boolean {
        if (!isAutoPTThold) {
            return false
        }
        if (SoundboardFrame.soundboard.entriesContainPTTKeys(pttkeys)) {
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(
                    null,
                    "A soundboard entry is using a key that conflicts with a 'Push to Talk' key. \n Disable 'Auto-hold PTT keys', or edit the entry or PTT keys.",
                    "Alert!", 0
                )
            }
            return false
        }
        if (currentlyPlayingClipCount == 0) {
            val robot = robotInstance
            for (i in pttkeys) {
                if (wasKeyLastPressedByRobot(
                        getKeyEventText(
                                i
                            )
                    )
                ) {
                    robot?.keyRelease(i)
                    println("Robot released: " + KeyEvent.getKeyText(i))
                }
            }
        }
        return true
    }

    @Synchronized
    fun incrementCurrentClipCount() {
        currentlyPlayingClipCount += 1
    }

    @Synchronized
    fun decrementCurrentClipCount() {
        if (currentlyPlayingClipCount >= 1) {
            currentlyPlayingClipCount -= 1
        }
    }

    @Synchronized
    fun zeroCurrentClipCount() {
        currentlyPlayingClipCount = 0
    }

    fun stringToIntArrayList(string: String): ArrayList<Int> {
        val array = ArrayList<Int>()
        val arrayString = string.replace('[', ' ').replace(']', ' ').trim()
        val numberString = arrayString.split(",").toTypedArray()

        for (i in numberString.indices) {
            if (numberString[i] != "") {
                val i2 = numberString[i].trim().toInt()
                array.add(i2)
            }
        }

        return array
    }

    fun submitNativeKeyPressTime(key: String, time: Long) {
        lastNativeKeyPressMap[key.lowercase(Locale.getDefault())] = time
    }

    private fun submitRobotKeyPressTime(key: String) {
        val time = System.currentTimeMillis()
        lastNativeKeyPressMap[key.lowercase(Locale.getDefault())] = time
        lastRobotKeyPressMap[key.lowercase(Locale.getDefault())] = time
    }

    private fun getLastNativeKeyPressTimeForKey(keyName: String): Long {
        return lastNativeKeyPressMap[keyName.lowercase(Locale.getDefault())] ?: return 0L
    }

    private fun getLastRobotKeyPressTimeForKey(keyName: String): Long {
        return lastRobotKeyPressMap[keyName.lowercase(Locale.getDefault())] ?: return 0L
    }

    private fun wasKeyLastPressedByRobot(keyName: String): Boolean {
        val human = getLastNativeKeyPressTimeForKey(keyName)
        val robot = getLastRobotKeyPressTimeForKey(keyName)
        return robot == human
    }

    fun isOverlapSameClipWhilePlaying(): Boolean {
        return overlapSameClipWhilePlaying
    }

    private fun stopFilePlaying(file: File): Boolean {
        var stopped = false
        val filepath = file.toString()
        val threads = arrayOfNulls<Thread>(clipPlayerThreadGroup.activeCount())

        clipPlayerThreadGroup.enumerate(threads)
        println("Thread count: " + threads.size)
        println("Thread groups: " + clipPlayerThreadGroup.activeGroupCount())
        println("Requesting: $filepath to stop")

        threads.forEach {
            if (it == null)
                return@forEach

            println("thread name: " + it.name)
            if (filepath == it.name) {
                val cp = it as ClipPlayer
                cp.stopPlaying()
                stopped = true
            }
        }

        return stopped
    }

    private class ClipPlayer(
        var file: File,
        primarySpeaker: SourceDataLine?,
        secondarySpeaker: SourceDataLine?
    ) : Thread(file.toString()) {

        var primarySpeaker: SourceDataLine? = null
        var secondarySpeaker: SourceDataLine? = null
        var playing = true

        init {
            this.primarySpeaker = primarySpeaker
            this.secondarySpeaker = secondarySpeaker
        }

        override fun run() {
            playSoundClip(file, primarySpeaker, secondarySpeaker)
        }

        fun stopPlaying() {
            println("Stopping clip: " + file.name)
            playing = false
        }

        private fun playSoundClip(file: File, primarySpeaker: SourceDataLine?, secondarySpeaker: SourceDataLine?) {
            PLAYALL = true

            var clip: AudioInputStream? = null
            val audioFormat: AudioFormat?

            try {
                clip = AudioSystem.getAudioInputStream(file)
                audioFormat = clip.format

                if (audioFormat != format)
                    clip = AudioSystem.getAudioInputStream(format, clip)
            } catch (e: UnsupportedAudioFileException) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(
                    null,
                    "${file.name} uses an unsupported format.",
                    "Unsupported Format",
                    0
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (clip != null) {
                incrementCurrentClipCount()

                val buffer = ByteArray(BUFFERSIZE)
                var bytesRead = 0

                while (playing && PLAYALL) {
                    try {
                        bytesRead = clip.read(buffer, 0, BUFFERSIZE)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    checkAndUseAutoPPThold()

                    if (bytesRead > 0) {
                        primarySpeaker?.write(buffer, 0, bytesRead)
                        secondarySpeaker?.write(buffer, 0, bytesRead)
                    }

                    if (bytesRead < BUFFERSIZE) {
                        playing = false
                    }
                }

                decrementCurrentClipCount()
                checkAndReleaseHeldPPTKeys()
            }

            if (clip != null) {
                try {
                    clip.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            primarySpeaker?.close()
            secondarySpeaker?.close()
        }
    }
}
