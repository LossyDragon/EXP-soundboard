package ca.exp.soundboard.rewrite.soundboard

import java.lang.NullPointerException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import javax.sound.sampled.*
import javax.swing.JOptionPane
import kotlin.math.abs
import kotlin.math.log10

class MicInjector internal constructor() : Thread() {

    // private val driftinterval = 1800000L
    private val inputBuffer: ByteArray = ByteArray(bufferSize)
    private var bytesRead = 0
    private var fadeOut = false
    private var gainControl: FloatControl? = null
    private var inputMixer: Mixer? = null
    private var isBypassing = false
    private var isMuted = false
    private var nextDrift: Long = 0
    private var outputMixer: Mixer? = null
    private var selectedInputLineName: String
    private var selectedOutputLineName: String
    private var sourceDataLine: SourceDataLine? = null
    private var targetDataLine: TargetDataLine? = null
    private var userVolume = 0

    var isRunning = false
        private set

    init {
        // TODO: fix character
        selectedInputLineName = "none selected"
        selectedOutputLineName = "none selected"
    }

    @Synchronized
    fun setGain(level: Float) {
        gain = level
        if (gainControl != null) {
            gainControl!!.value = level
        }
    }

    @Synchronized
    fun setInputMixer(mixerName: String) {
        val mixers = getMixerNames(targetDataLineInfo)
        val j = mixers.size

        for (i in 0 until j) {

            var arrayOfInfo: Array<Mixer.Info>
            val m = AudioSystem.getMixerInfo().also { arrayOfInfo = it }.size

            for (k in 0 until m) {
                val mixerInfo = arrayOfInfo[k]
                if (mixerName == mixerInfo.name) {
                    inputMixer = AudioSystem.getMixer(mixerInfo)
                    return
                }
            }
        }
    }

    @Synchronized
    fun setOutputMixer(mixerName: String) {
        val mixers = getMixerNames(sourceDataLineInfo)
        val j = mixers.size

        for (i in 0 until j) {

            var arrayOfInfo: Array<Mixer.Info>
            val m = AudioSystem.getMixerInfo().also { arrayOfInfo = it }.size

            for (k in 0 until m) {
                val mixerInfo = arrayOfInfo[k]

                if (mixerName == mixerInfo.name) {
                    outputMixer = AudioSystem.getMixer(mixerInfo)
                    return
                }
            }
        }
    }

    @Synchronized
    fun setupGate() {
        if (targetDataLine != null) {
            clearLines()
        }

        try {
            targetDataLine = inputMixer!!.getLine(targetDataLineInfo) as TargetDataLine
            selectedInputLineName = inputMixer!!.mixerInfo.name
            targetDataLine!!.open(signedFormat, INTERNAL_BUFFER_SIZE)
            targetDataLine!!.start()
        } catch (ex: LineUnavailableException) {
            JOptionPane.showMessageDialog(
                null,
                "Selected Input Line $selectedInputLineName is currently unavailable.",
                "Line Unavailable Exception", 0
            )
        }

        try {
            sourceDataLine = outputMixer!!.getLine(sourceDataLineInfo) as SourceDataLine
            selectedOutputLineName = outputMixer!!.mixerInfo.name
            sourceDataLine!!.open(signedFormat, INTERNAL_BUFFER_SIZE)
            sourceDataLine!!.start()
        } catch (ex: LineUnavailableException) {
            JOptionPane.showMessageDialog(
                null,
                "Selected Output Line $selectedOutputLineName is currently unavailable.",
                "Line Unavailable Exception",
                0
            )
        }

        gainControl = sourceDataLine!!.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        gainControl!!.value = gain

        println(targetDataLine!!.lineInfo.toString())
        println("Buffer size is " + targetDataLine!!.bufferSize)

        fadeOut = true
    }

    @Synchronized
    private fun clearLines() {
        targetDataLine!!.close()
        sourceDataLine!!.close()
    }

    private fun read() {
        bytesRead = targetDataLine!!.read(inputBuffer, 0, bufferSize)
    }

    private fun write() {
        sourceDataLine!!.write(inputBuffer, 0, bytesRead)
    }

    private fun writeFadeIn() {
        sourceDataLine!!.write(inputBuffer, 0, bytesRead)

        if (gainControl!!.value < userVolume) {
            if (gainControl!!.value < -20.0f) {
                gainControl!!.value = -20.0f
            }

            gainControl!!.shift(gainControl!!.value, gainControl!!.value + 1.0f, 10000000)
        }

        if (gainControl!!.value >= userVolume) {
            resetGain()
        }
    }

    private fun writeFadeOut() {
        sourceDataLine!!.write(inputBuffer, 0, bytesRead)

        if (fadeOut) {
            if (gainControl!!.value > -70.0f) {
                gainControl!!.value = gainControl!!.value - 0.1f
            }

            if (gainControl!!.value <= -69.9f) {
                gainControl!!.value = -80.0f

                fadeOut = false

                println("Fade OUT off!")
            }
        }
    }

    override fun run() {
        setupGate()

        isRunning = true
        nextDrift = System.currentTimeMillis() + 1800000L

        while (isRunning) {
            read()
            write()
            if (System.currentTimeMillis() > nextDrift) {
                driftReset()
            }
        }
    }

    @Synchronized
    fun setBypass(bypass: Boolean) {
        isBypassing = bypass
    }

    @Synchronized
    fun setFadeOut(fadeOut: Boolean) {
        this.fadeOut = fadeOut
    }

    @Synchronized
    fun setMute(mute: Boolean) {
        isMuted = mute
        if (isMuted) {
            isBypassing = false
            fadeOut = true
        }
    }

    private fun resetGain() {
        gainControl!!.value = userVolume.toFloat()
    }

    fun stopRunning() {
        isRunning = false
    }

    @Synchronized
    private fun driftReset() {
        if (System.currentTimeMillis() > nextDrift) {
            nextDrift = System.currentTimeMillis() + 1800000L

            try {
                targetDataLine!!.open(signedFormat, INTERNAL_BUFFER_SIZE)
                targetDataLine!!.start()
            } catch (ex: LineUnavailableException) {
                JOptionPane.showMessageDialog(
                    null,
                    "Selected Input Line is currently unavailable",
                    "Line Unavailable Exception",
                    0
                )
            }

            try {
                sourceDataLine!!.open(signedFormat, INTERNAL_BUFFER_SIZE)
                sourceDataLine!!.start()
            } catch (ex: LineUnavailableException) {
                JOptionPane.showMessageDialog(
                    null,
                    "Selected Output Line is currently unavailable.",
                    "Line Unavailable Exception",
                    0
                )
            }

            println("DriftReset")
        }
    }

    companion object {
        private const val INTERNAL_BUFFER_SIZE = 8192
        private const val bufferSize = 512
        private const val fFrameRate = 44100.0f
        private val signedFormat = AudioFormat(
            /* encoding = */ AudioFormat.Encoding.PCM_SIGNED,
            /* sampleRate = */ fFrameRate,
            /* sampleSizeInBits = */ 16,
            /* channels = */ 2,
            /* frameSize = */ 4,
            /* frameRate = */ fFrameRate,
            /* bigEndian = */ false
        )

        val targetDataLineInfo = DataLine.Info(TargetDataLine::class.java, signedFormat, INTERNAL_BUFFER_SIZE)
        val sourceDataLineInfo = DataLine.Info(SourceDataLine::class.java, signedFormat, INTERNAL_BUFFER_SIZE)

        @get:Synchronized
        var gain = 0f
            private set

        fun getMixerNames(lineInfo: DataLine.Info?): Array<String?> {
            val mixerNames = ArrayList<String>()
            val info = AudioSystem.getMixerInfo()
            var arrayOfInfo1: Array<Mixer.Info>
            val j = info.also { arrayOfInfo1 = it }.size

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

            val returnarray = arrayOfNulls<String>(mixerNames.size)
            return mixerNames.toArray(returnarray)
        }

        private fun findLevel(buffer: ByteArray): Float {
            var dB = 0.0
            for (i in buffer.indices) {
                dB = 20.0 * log10(abs(buffer[i] / 32767.0))

                if (dB == Double.NEGATIVE_INFINITY || dB.isNaN()) {
                    // (dB == NaN.0D)) { // TODO: fix this value
                    dB = -90.0
                }
            }

            return dB.toFloat() + 91.0f
        }

        fun getdB(buffer: ByteArray): Float {
            var dB = 0.0
            val shortArray = ShortArray(buffer.size / 2)

            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()[shortArray]

            for (i in shortArray.indices) {
                dB = 20.0 * log10(abs(shortArray[i] / 32767.0))

                if (dB == Double.NEGATIVE_INFINITY || dB.isNaN()) {
                    // (dB == NaN.0D)) { // TODO: fix this value
                    dB = -90.0
                }
            }

            return dB.toFloat() + 91.0f
        }

        fun byteToShortArray(byteArray: ByteArray): ShortArray {
            val shortArray = ShortArray(byteArray.size / 2)

            for (i in shortArray.indices) {
                val ub1: Int = byteArray[i * 2 + 0].toInt() and 0xFF
                val ub2: Int = byteArray[i * 2 + 1].toInt() and 0xFF
                shortArray[i] = ((ub2 shl 8) + ub1).toShort()
            }

            return shortArray
        }

        fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
            val byteArray = ByteArray(shortArray.size * 2)

            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortArray)

            return byteArray
        }
    }
}
