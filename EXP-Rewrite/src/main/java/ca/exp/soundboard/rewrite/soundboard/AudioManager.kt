package ca.exp.soundboard.rewrite.soundboard

import ca.exp.soundboard.rewrite.soundboard.Utils.getMixerNames
import java.io.File
import javax.sound.sampled.*
import javax.swing.JOptionPane

class AudioManager {

    private var primaryOutput: Mixer? = null
    private var secondaryOutput: Mixer? = null
    private var useSecondary = false
    val standardDataLineInfo = DataLine.Info(SourceDataLine::class.java, Utils.format, 2048)

    fun playSoundClip(file: File, halfSpeed: Boolean) {
        val format: AudioFormat? = if (halfSpeed) Utils.modifiedPlaybackFormat else Utils.format

        if (file.exists() && file.canRead()) {
            var primarySpeaker: SourceDataLine? = null
            var secondarySpeaker: SourceDataLine? = null

            try {
                primarySpeaker = primaryOutput!!.getLine(standardDataLineInfo) as SourceDataLine
                primarySpeaker.open(format, 8192)

                val gain = primarySpeaker.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                gain.value = firstOutputGain

                primarySpeaker.start()
            } catch (ex: LineUnavailableException) {
                JOptionPane.showMessageDialog(
                    /* parentComponent = */ null,
                    /* message = */ "Selected Output Line: Primary Speaker is currently unavailable.",
                    /* title = */ "Line Unavailable Exception",
                    /* messageType = */ 0
                )
            }

            if (secondaryOutput != null && useSecondary) {
                try {
                    secondarySpeaker = secondaryOutput!!.getLine(standardDataLineInfo) as SourceDataLine
                    secondarySpeaker.open(format, 8192)

                    val gain = secondarySpeaker.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    gain.value = secondOutputGain

                    secondarySpeaker.start()
                } catch (ex: LineUnavailableException) {
                    JOptionPane.showMessageDialog(
                        /* parentComponent = */ null,
                        /* message = */ "Selected Output Line: Secondary Speaker is currently unavailable.",
                        /* title = */ "Line Unavailable Exception",
                        /* messageType = */ 0
                    )
                }
            }

            Utils.playNewSoundClipThreaded(file, primarySpeaker, secondarySpeaker)
        }
    }

    @Synchronized
    fun setPrimaryOutputMixer(mixerName: String) {
        val mixers = getMixerNames(standardDataLineInfo)

        for (i in mixers.indices) {
            var arrayOfInfo: Array<Mixer.Info>
            val m = AudioSystem.getMixerInfo().also { arrayOfInfo = it }.size

            for (k in 0 until m) {
                val mixerInfo = arrayOfInfo[k]
                if (mixerName == mixerInfo.name) {
                    primaryOutput = AudioSystem.getMixer(mixerInfo)

                    return
                }
            }
        }
    }

    fun setUseSecondary(use: Boolean) {
        useSecondary = use
    }

    fun useSecondary(): Boolean {
        return useSecondary
    }

    @Synchronized
    fun setSecondaryOutputMixer(mixerName: String) {
        val mixers = getMixerNames(standardDataLineInfo)

        for (i in mixers.indices) {
            var arrayOfInfo: Array<Mixer.Info>
            val info = AudioSystem.getMixerInfo().also { arrayOfInfo = it }.size

            for (k in 0 until info) {
                val mixerInfo = arrayOfInfo[k]
                if (mixerName == mixerInfo.name) {
                    secondaryOutput = AudioSystem.getMixer(mixerInfo)

                    return
                }
            }
        }
    }

    companion object {
        // private const val INTERNAL_BUFFER_SIZE = 8192
        private var firstOutputGain = 0f
        private var secondOutputGain = 0f

        fun getFirstOutputGain(): Float {
            return firstOutputGain
        }

        fun setFirstOutputGain(firstOutputGain: Float) {
            this.firstOutputGain = firstOutputGain
        }

        fun getSecondOutputGain(): Float {
            return secondOutputGain
        }

        fun setSecondOutputGain(secondOutputGain: Float) {
            this.secondOutputGain = secondOutputGain
        }
    }
}
